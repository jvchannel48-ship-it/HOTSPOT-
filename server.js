const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const { rateLimit } = require("express-rate-limit");

const app = express();
const PORT = Number(process.env.PORT || 3000);
const TMDB_API_TOKEN = process.env.TMDB_API_TOKEN;
const TMDB_BASE_URL = "https://api.themoviedb.org";

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
  res.type("text").send("CINE-KO API is running. See /healthz.");
});

app.get("/healthz", (_req, res) => {
  res.json({
    ok: true,
    service: "cine-ko-api",
    tmdbConfigured: Boolean(TMDB_API_TOKEN)
  });
});

// Proxies TMDB metadata requests without exposing the TMDB bearer token to the app.
// Example: /api/tmdb/3/movie/popular?language=en-US&page=1
app.get("/api/tmdb/*", async (req, res) => {
  if (!TMDB_API_TOKEN) {
    return res.status(503).json({
      error: "TMDB_API_TOKEN is not configured on the server."
    });
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
