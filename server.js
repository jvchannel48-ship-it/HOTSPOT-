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
app.use(helmet({
  crossOriginResourcePolicy: false,
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", "data:", "https://image.tmdb.org"],
      frameSrc: ["https://www.youtube-nocookie.com"],
      connectSrc: ["'self'"],
      upgradeInsecureRequests: null
    }
  }
}));

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


// Keyless public Cinemeta metadata proxy (no TMDB token required).
app.get("/api/cinemeta/*", async (req, res) => {
  const requestedPath = req.params[0] || "";
  if (!/^(catalog|meta)\/[a-zA-Z0-9_./=%+-]+\.json$/.test(requestedPath) ||
      requestedPath.includes("..")) {
    return res.status(400).json({ error: "Invalid Cinemeta path." });
  }
  try {
    const upstream = await fetch("https://v3-cinemeta.strem.io/" + requestedPath, {
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(15000)
    });
    const body = await upstream.text();
    res.status(upstream.status);
    res.type(upstream.headers.get("content-type") || "application/json");
    res.set("Cache-Control", "public, max-age=300");
    return res.send(body);
  } catch (error) {
    console.error("Cinemeta proxy error:", error.message);
    return res.status(502).json({ error: "Public metadata service is temporarily unavailable." });
  }
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


/* Public video search and playback links from Internet Archive.
   Only return files whose item metadata declares public-domain or Creative Commons rights. */
app.get("/api/videos/search", async (req, res) => {
  const q = String(req.query.q || "feature film").trim().slice(0, 100);
  if (!q) return res.status(400).json({ error: "Enter a search term." });
  try {
    const params = new URLSearchParams({
      q: `mediatype:movies AND (${q.replace(/[+\-!(){}\[\]^"~*?:\\/]/g, " " )})`,
      "fl[]": "identifier,title,year,description,licenseurl",
      rows: "24",
      page: "1",
      output: "json",
      "sort[]": "downloads desc"
    });
    const upstream = await fetch("https://archive.org/advancedsearch.php?" + params, {
      headers: { Accept: "application/json" }, signal: AbortSignal.timeout(20000)
    });
    if (!upstream.ok) throw new Error("Internet Archive search failed");
    const data = await upstream.json();
    return res.json({ results: (data.response?.docs || []).map(x => ({
      id: x.identifier, title: x.title || x.identifier, year: x.year || "",
      description: x.description || "", license: x.licenseurl || ""
    })) });
  } catch (error) {
    console.error("Archive video search:", error.message);
    return res.status(502).json({ error: "Video catalog is temporarily unavailable." });
  }
});

app.get("/api/videos/:identifier", async (req, res) => {
  const id = String(req.params.identifier || "");
  if (!/^[a-zA-Z0-9._-]{1,120}$/.test(id)) return res.status(400).json({ error: "Invalid video id." });
  try {
    const upstream = await fetch("https://archive.org/metadata/" + encodeURIComponent(id), {
      headers: { Accept: "application/json" }, signal: AbortSignal.timeout(20000)
    });
    if (!upstream.ok) throw new Error("Internet Archive metadata failed");
    const data = await upstream.json();
    const license = String(data.metadata?.licenseurl || data.metadata?.license || "").toLowerCase();
    if (!(license.includes("publicdomain") || license.includes("creativecommons.org"))) {
      return res.status(403).json({ error: "This item does not declare a public-domain or Creative Commons license, so playback is not enabled." });
    }
    const files = (data.files || []).filter(f =>
      /\.(mp4|m4v|webm)$/i.test(f.name || "") &&
      !f.name.includes("__ia_thumb") && !f.source?.includes("derivative")
    ).sort((a,b) => Number(b.size || 0) - Number(a.size || 0));
    const file = files[0];
    if (!file) return res.status(404).json({ error: "No browser-playable MP4, M4V, or WebM file was found." });
    return res.json({
      id, title: data.metadata?.title || id, license,
      videoUrl: "https://archive.org/download/" + encodeURIComponent(id) + "/" + encodeURIComponent(file.name),
      format: file.format || file.name.split(".").pop().toUpperCase()
    });
  } catch (error) {
    console.error("Archive video metadata:", error.message);
    return res.status(502).json({ error: "Unable to retrieve this video right now." });
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
