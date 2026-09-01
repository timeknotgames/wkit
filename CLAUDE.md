# WKit — Worlding Kit

A Paper plugin that generates real-world terrain into a Minecraft server on
demand, from OpenStreetMap data and elevation tiles. Java, Gradle Kotlin DSL,
package `fun.eightxm.wkit`.

Built for 7ABCs.com / tek8.org: urban planning, education and governance in the
hands of children, partnered with Traditional Ecological Knowledges. The world
origin is **Mount Tahoma (Mother of Waters)** summit, `46.8523°N, 121.7603°W`,
mapped to Minecraft `(0,0)`.

## Relationship to Arnis

This project is inspired by [Arnis](https://github.com/louis-e/arnis)
(Apache-2.0), which generates Minecraft worlds from OSM data as a standalone
Rust application. WKit extends the idea into a live Paper `ChunkGenerator` that
renders terrain as players explore. Arnis's approach to OSM element processing,
elevation handling and block mapping informed WKit's design throughout.

WKit contains **no Arnis source**. It is original Java, licensed Apache-2.0 to
match. See `README.md` for full attribution.

## Layout

- `src/main/java/fun/eightxm/wkit/`
  - `WKitPlugin.java` — plugin entry point
  - `generator/` — `ArnisChunkGenerator`, `ElevationCache`, `ElevationMapper`,
    `OSMCache`, `OverpassFetcher`, `TileFetcher`
  - `sector/` — `SectorRegistry`, `Sector`, `SectorPlacement`, `GeoProjection`
  - `region/` — `RegionMath`
  - `arnis/` — `ArnisMetadata`
  - `tools/` — `CoordsTool`
- `src/main/resources/plugin.yml`
- `src/test/java/...` — 11 test classes mirroring the packages above
- `.github/workflows/ci.yml`
- `research/`, `LAND_ACKNOWLEDGEMENT.md`

## Commands

```bash
./gradlew build      # compile + run tests
./gradlew test
```

## Runtime state lives on the server, not here

The live plugin configuration — `config.yml` and `sectors.json` — belongs to the
server deployment, under `plugins/WKit/`. It is not part of this repository.

`plugins/WKit/osm-data/` and `plugins/WKit/tile-cache/` are regenerated from
Overpass and elevation tile sources; they are caches and should never be
committed.

## Traps

**`build/` and `.gradle/` are generated** and ignored — never commit them.

**Overpass is a shared public API.** `OverpassFetcher` hits it directly. Respect
rate limits and cache aggressively — that is what `OSMCache` is for. A chunk
generator that queries Overpass per chunk will get the server blocked.

**Coordinates are real places.** Sector placement maps to actual latitude and
longitude, and the land acknowledgement is part of this project rather than
decoration — see `LAND_ACKNOWLEDGEMENT.md`. The world origin is a specific
place with specific relationships.
