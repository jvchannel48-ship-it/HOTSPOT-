# CINE-KO

Android movie and series discovery app.

## Features
- Home, Movies, Series, Search, and My List navigation
- Movie and TV-series catalog powered by TMDB
- Search across movie and TV titles
- Series seasons and episodes displayed in episode-number order
- Official YouTube trailer/preview playback in the app
- Previous/Next episode navigation when episode previews are available
- Locally saved My List

## Build APK
The GitHub Actions workflow builds a debug APK on every push to `main`, and can also be started manually from **Actions → Android APK → Run workflow**. Download the APK from the workflow run's Artifacts section.

## TMDB API setup
CINE-KO uses The Movie Database (TMDB) API for catalog metadata, posters, series seasons, episodes, and official video listings.

1. Create a TMDB account at https://www.themoviedb.org/.
2. Request an API Read Access Token in Account Settings → API.
3. Install/open CINE-KO, tap the gear icon, and paste the Read Access Token.

The token is stored locally in the app's private preferences. Do not commit personal API tokens to this repository.

## Playback limitation
TMDB is a metadata/catalog API. It does **not** provide licensed full-length movie or episode streams. CINE-KO plays official YouTube trailers/previews returned by TMDB. Full movies and episodes require a licensed streaming provider or video URLs that you own or are authorized to distribute. Episode navigation follows the TMDB episode order; a next episode can play only when an official preview is available.

## Privacy
The app requests internet access to load catalog data and posters. Search and catalog requests are sent to TMDB. The app does not request root, VPN, hotspot, contacts, or location permissions.
