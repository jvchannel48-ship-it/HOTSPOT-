# CINE-KO backend (Render)

This repository contains both the Android app and a small Node.js API service.

## Render Web Service settings

- Runtime: Node
- Build Command: `npm install`
- Start Command: `npm start`
- Health Check Path: `/healthz`
- Region: Singapore
- Free plan is selected in render.yaml (availability and limits are controlled by Render).

## Required environment variable

In Render → CINE-KO API → Environment, add:

- `TMDB_API_TOKEN`: your TMDB API Read Access Token (keep it private; do not commit it to GitHub).

Optional:
- `CORS_ORIGINS`: comma-separated allowed web origins. The default in this starter configuration is `*`.

## Endpoints

- `GET /` – service status text
- `GET /healthz` – health status and whether the TMDB token is configured
- `GET /api/tmdb/3/movie/popular` – example TMDB metadata proxy
- `GET /api/tmdb/3/search/movie?query=...` – example search proxy

The service proxies TMDB metadata only. It does not provide movie or episode video streams or bypass licensing. Use only video sources you are authorized to play.
