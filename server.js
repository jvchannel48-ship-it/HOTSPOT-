const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const { rateLimit } = require("express-rate-limit");
const Redis = require("ioredis");
const path = require("path");

const app = express();
const PORT = Number(process.env.PORT || 3000);
const TMDB_API_TOKEN = process.env.TMDB_API_TOKEN;
const TMDB_BASE_URL = "https://api.themoviedb";
const redis = process.env.REDIS_URL
  ? new Redis(process.env.REDIS_URL, {
      lazyConnect: true,
      maxRetriesPerRequest: 1,
      retryStrategy: (times) => Math.min(times * 500, 5000)
    })
  : null;

if (redis) {
  redis.on("error", (error) => console.error("Redis connection error:", error.message));
  redis.connect().catch((error) => console.error("Redis initial connection failed:", error.message));
}

app.disable("x-powered-by");
app.use(helmet({ crossOriginResourcePolicy: false }));

const allowedOrigins = (process.env.CORS_ORIGINS || "*")
  .split(",").map((value) => value.trim()).filter(Boolean);

app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.includes("*") || allowedOrigins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error("Origin is not allowed by CORS"));
  }
}));

app.use(express.json({ limit: "32kb" }));
app.use("/api/", rateLimit({
  windowMs: 60 * 1000,
  limit: 60,
  standardHeaders: "draft-7",
  legacyHeaders: false
}));

app.get("/", (_req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

app.get("/healthz", (_req, res) => {
  res.json({
    ok: true,
    service: "cine-ko-api",
    tmdbConfigured: Boolean(TMDB_API_TOKEN),
    redisConfigured: Boolean(redis),
    redisReady: Boolean(redis && redis.status === "ready")
  });
});

// Metadata proxy only. TMDB does not provide full movie or episode streams.
app.get("/api/tmdb/*", async (req, res) => {
  if (!TMDB_API_TOKEN) {
    return res.status(503).json({ error: "TMDB_API_TOKEN is not configured on the server." });
  }

  const requestedPath = req.params[0] || "";
  if (!/^3\/[a-zA-Z0-9_./-]+$/.test(requestedPath) ||
      requestedPath.includes("..") || requestedPath.startsWith("/")) {
    return res.status(400).json({ error: "Invalid TMDB API path." });
  }

  try {
    const target = new URL("/" + requestedPath, TMDB_BASE_URL);
    for (const [key, value] of Object.entries(req.query)) {
      if (typeof value === "string") target.searchParams.set(key, value);
    }

    const cacheKey = "tmdb:" + target.toString();
    if (redis && redis.status === "ready") {
      try {
        const cached = await redis.get(cacheKey);
        if (cached) {
          return res.type("application/json").set("X-CINE-KO-Cache", "HIT").send(cached);
        }
      } catch (error) {
        console.error("Redis read failed:", error.message);
      }
    }

    const upstream = await fetch(target, {
      headers: {
        Authorization: `Bearer ${TMDB_API_TOKEN}`,
        Accept: "application/json"
      },
      signal: AbortSignal.timeout(15000)
    });

    const body = await upstream.text();
    res.status(upstream.status);
    res.type(upstream.headers.get("content-type") || "application/json");

    if (upstream.ok && redis && redis.status === "ready") {
      try {
        await redis.set(cacheKey, body, "EX", 300);
      } catch (error) {
        console.error("Redis write failed:", error.message);
      }
    }

    res.set("X-CINE-KO-Cache", "MISS");
    return res.send(body);
  } catch (error) {
    console.error("TMDB proxy error:", error.message);
    return res.status(502).json({ error: "Unable to reach TMDB right now." });
  }
});

app.use((err, _req, res, _next) => {
  if (err.message === "Origin is not allowed by CORS") {
    return res.status(403).json({ error: err.message });
  }
  console.error(err);
  return res.status(500).json({ error: "Internal server error." });
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`CINE-KO API listening on port ${PORT}`);
});
