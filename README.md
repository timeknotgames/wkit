# WKit — The Living Map Engine

**A Paper Minecraft plugin that generates real-world terrain, buildings, roads, and data layers as you explore.**

WKit is a custom `ChunkGenerator` for Paper 1.21.x that renders the real Earth in Minecraft at 1:1 scale using live data from OpenStreetMap and AWS elevation tiles. Walk from Mount Tahoma's snow-capped summit down through temperate rainforests to downtown Tacoma — all generated seamlessly as you move.

Part of the [TEK8 Learning Lotus](https://tek8.org) educational framework and the [7ABCs](https://7abcs.com) initiative for putting urban planning, education, and governance tools in the hands of children.

## Features

- **Real-world elevation** from AWS Terrarium tiles with piecewise compression (1:1 for cities, compressed for mountains — walkable everywhere)
- **OSM buildings** rendered as structures with heights based on building type
- **OSM roads** with correct widths and materials (motorway → residential → footpath)
- **Water bodies** — oceans, rivers, lakes at correct positions
- **Biome-appropriate vegetation** — Pacific NW temperate rainforest, montane conifers, alpine meadows, glaciers
- **Proper biome assignment** from elevation (PLAINS → OLD_GROWTH_SPRUCE_TAIGA → TAIGA → SNOWY_SLOPES → FROZEN_PEAKS)
- **Geographic projection** centered on Mount Tahoma (Mother of Waters) at MC (0,0)
- **Fully concurrent** — thread-safe chunk generation, async OSM/elevation fetching with caching
- **Live exploration** — the entire Earth is explorable, generated on-demand

## Origin

Mount Tahoma (Mount Rainier), known as the Mother of Waters, stands at the center of this world at Minecraft coordinates (0, 0). All geography is positioned at its true relative distance and direction from the mountain.

## Credits & Acknowledgments

- **[Arnis](https://github.com/louis-e/arnis)** (Apache 2.0) — The original inspiration. Arnis generates Minecraft worlds from OSM data as a standalone Rust application. WKit extends this concept into a live Paper `ChunkGenerator` that renders terrain on-demand as players explore. Arnis's approach to OSM element processing, elevation handling, and block mapping informed WKit's design throughout.

- **[OpenStreetMap](https://www.openstreetmap.org/)** (ODbL) — All geographic features (buildings, roads, water, landuse, parks) are sourced from OSM via the Overpass API. OSM's community-maintained global dataset makes this project possible.

- **[AWS Terrarium Elevation Tiles](https://registry.opendata.aws/terrain-tiles/)** — Elevation data powering the terrain heightmap.

- **[FastAsyncWorldEdit](https://github.com/IntellectualSites/FastAsyncWorldEdit)** (GPL) — FAWE's async block placement API was explored during development. The final architecture uses Paper's native ChunkGenerator API instead.

- **[Paper](https://papermc.io/)** — The high-performance Minecraft server that provides the ChunkGenerator API making live terrain generation possible.

- **[TEK8 Framework](https://tek8.org)** — The 8-petal Learning Lotus and Crystal Cycle that structure how data layers map to educational outcomes. TEK8 grounds this work in Traditional Ecological Knowledges, Indigenous cartographies, and Ivan Illich's Tools for Conviviality.

- **[Build The Earth](https://buildtheearth.net/)** / **[Terra++](https://github.com/BuildTheEarth/terraplusplus)** — Prior art for 1:1 Earth generation in Minecraft. Terra++ demonstrated the concept on Forge 1.12.2; WKit brings it to modern Paper servers with data layer integration.

- **[Native Land Digital](https://native-land.ca/)** (CC BY-NC-SA) — Indigenous territory data for the territorial sovereignty overlay (planned).

## Data Sources

| Layer | Source | License |
|-------|--------|---------|
| Elevation | AWS Terrarium Tiles | Public |
| Buildings, roads, water | OpenStreetMap Overpass API | ODbL |
| Forests | Global Forest Watch | CC BY 4.0 |
| Water resources | USGS NWIS | Public Domain |
| Watersheds | USGS WBD | Public Domain |
| Indigenous territories | Native Land Digital | CC BY-NC-SA |
| Waste/Superfund | EPA Envirofacts | Public Domain |
| Mining | USGS MRDS | Public Domain |

*60+ additional datasets planned — see research papers.*

## Research Papers

- [Terrain Integration](research/WKIT_TERRAIN_INTEGRATION_v1.md) — Methods and challenges
- [Data Layer Architecture](research/WKIT_DATA_LAYERS_v1.md) — 60+ datasets organized into 7 tiers
- [TEK8 Integrated Framework](research/TEK8_WKIT_INTEGRATED_FRAMEWORK_v1.md) — The full vision

## Requirements

- Paper 1.21.8+
- Java 21+
- Internet access (for Overpass API and AWS elevation tiles)

## Building

```bash
./gradlew build
# Output: build/libs/wkit-1.0.0-SNAPSHOT.jar
```

## Installation

1. Place `wkit-1.0.0-SNAPSHOT.jar` in your Paper server's `plugins/` directory
2. Start the server — WKit creates an `arnis_world` with the custom generator
3. Connect and teleport: `/execute in minecraft:arnis_world run tp @s 0 300 0` (Mount Tahoma summit)
4. Explore downtown Tacoma: `/execute in minecraft:arnis_world run tp @s -51900 50 -44600`
5. Walk anywhere — the entire Earth generates as you move

## License

Apache 2.0 (matching Arnis)

## Community

- **tek8.org** — TEK8 Learning Lotus framework
- **7abcs.com** — 7ABCs afterschool initiative
- **skool.com/7abcs** — Community discussion

---

*For the children who will inherit the Earth.*
