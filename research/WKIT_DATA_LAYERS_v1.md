# WKit Data Layer Architecture: From Terrain to Territorial Sovereignty

**Authors:** JaiRuviel (7ABCs.com / tek8.org), Claude Code (Anthropic)
**Date:** 2026-03-18
**Version:** 1.0 — Working Paper
**Project:** WKit (Worlding Kit) — Data Layer Integration for Educational Minecraft Server Networks
**Companion to:** WKIT_TERRAIN_INTEGRATION_v1.md

---

## Abstract

Building on our successful FAWE-based terrain integration (561,327 blocks placed live from Arnis-generated OSM/elevation data), this paper catalogs the public datasets, GIS tools, and architectural patterns needed to overlay real-world data onto Minecraft servers as a convivial educational tool. We organize data layers into a priority hierarchy — starting with forests, water, waste, and extractive industry — and propose an architecture connecting PostGIS, GeoServer, QGIS, Google My Maps, and a custom Paper plugin to render these layers as living, queryable geography in Minecraft.

The ultimate goal: a Minecraft server where every block carries metadata about the land it represents — who lived here first, what treaties govern it, what's been extracted from it, what's been dumped on it, where the minerals in your computer came from, and where your e-waste will end up.

---

## 1. The FAWE Breakthrough

On 2026-03-18 we proved that Arnis-generated terrain CAN be placed into a live Paper server through FastAsyncWorldEdit's block placement API:

```
[01:36:47] Arnis generated: 304x333 blocks
[01:36:47] Pasting terrain via FAWE (304x333 blocks)...
[01:36:48] FAWE EditSession: placed 561327 blocks, flushing...
[01:36:54] Sector 'Tahoma_FAWE' is LIVE!
```

The key insight: bypass Arnis's .mca file output entirely. Read blocks from Arnis's NBT data using Querz, place them through FAWE/WorldEdit's API, and let Paper create properly formatted chunks. This works because blocks go through the Bukkit API — Paper handles NBT serialization, heightmaps, lighting, and all the format requirements that direct .mca injection fails on.

---

## 2. Data Layer Hierarchy

### Tier 1 — Environmental Foundation (Start Here)

| Layer | Primary Source | API | Format | License |
|-------|---------------|-----|--------|---------|
| **Forests** | Global Forest Watch | REST (data-api.globalforestwatch.org) | GeoTIFF, GeoJSON, CSV | CC BY 4.0 |
| **Water resources** | USGS NWIS | REST (waterservices.usgs.gov) | JSON, WaterML | Public Domain |
| **Watersheds** | USGS WBD + NHD | WFS, Download | Shapefile, GeoJSON | Public Domain |
| **Active fires** | NASA FIRMS | REST (firms.modaps.eosdis.nasa.gov) | CSV, KML, GeoJSON | Public Domain |
| **Land cover** | USGS NLCD/MRLC | WMS, Download | GeoTIFF 30m | Public Domain |

### Tier 2 — Waste and Extraction

| Layer | Primary Source | API | Format | License |
|-------|---------------|-----|--------|---------|
| **Superfund sites** | EPA Envirofacts | REST (data.epa.gov) | JSON, CSV | Public Domain |
| **Toxic releases** | EPA TRI | REST (data.epa.gov) | JSON, CSV | Public Domain |
| **Landfills** | EPA LMOP | Download | CSV, Excel | Public Domain |
| **Mining operations** | USGS MRDS | WMS/WFS (mrdata.usgs.gov) | Shapefile, CSV | Public Domain |
| **Fracking wells** | FracFocus | Download | CSV | Public |
| **Coal/oil/gas** | Global Energy Monitor | Download | CSV, KML | CC BY 4.0 |

### Tier 3 — Infrastructure and Digital

| Layer | Primary Source | API | Format | License |
|-------|---------------|-----|--------|---------|
| **Power grid** | Open Infrastructure Map (OSM) | Overpass API | JSON | ODbL |
| **Dams** | GRanD + Global Dam Tracker | Download | Shapefile, CSV | Academic/CC BY |
| **Pipelines** | Global Energy Monitor | Download | CSV, KML | CC BY 4.0 |
| **Data centers** | OSM (`man_made=data_center`) + EIA | Overpass + REST | JSON, CSV | ODbL/Public Domain |
| **Crypto mining** | CBECI (Cambridge) | Download | CSV, JSON | Academic |
| **Submarine cables** | TeleGeography | GitHub GeoJSON | GeoJSON | Permissive |
| **Broadband** | FCC Broadband Map | REST, Download | CSV, Shapefile | Public Domain |

### Tier 4 — Territorial Sovereignty

| Layer | Primary Source | API | Format | License |
|-------|---------------|-----|--------|---------|
| **Indigenous territories** | Native Land Digital | REST (native-land.ca/api) | GeoJSON | CC BY-NC-SA |
| **Tribal boundaries (US)** | Census TIGER AIANNH | REST (TIGERweb), Download | Shapefile, GeoJSON | Public Domain |
| **Global Indigenous lands** | LandMark (WRI) | Download, Resource Watch API | Shapefile, GeoJSON | CC BY 4.0 |
| **Treaties** | Native Land Digital | REST | GeoJSON | CC BY-NC-SA |
| **Federal land ownership** | USGS PAD-US | ArcGIS REST, Download | Geodatabase, Shapefile | Public Domain |
| **Protected areas (global)** | WDPA/Protected Planet | Download + API | Shapefile | CC BY-NC |
| **Maritime zones** | Marine Regions (Flanders) | WFS, REST | Shapefile, GeoJSON | CC BY |
| **UNDRIP compliance** | IWGIA, Cultural Survival | No API (reports) | PDF | Research |

### Tier 5 — Supply Chain: Extraction to Disposal

| Layer | Primary Source | API | Format | License |
|-------|---------------|-----|--------|---------|
| **Conflict minerals** | SEC EDGAR + RMI smelter list | EDGAR API, Download | XML, Excel | Public Domain |
| **Rare earth mining** | USGS MRDS | WMS/WFS | Shapefile, CSV | Public Domain |
| **Semiconductor fabs** | Wikipedia + CHIPS Act data | No API | HTML, PDF | CC BY-SA / Public |
| **E-waste flows** | Global E-waste Monitor + BAN | Download | CSV, KML | Research |
| **Recycling facilities** | Earth911 + EPA RCRAInfo | REST (Envirofacts) | JSON, CSV | Public Domain |
| **Repairability** | iFixit | REST (ifixit.com/api/2.0/) | JSON | CC BY-NC-SA |
| **Supply hub** | Open Supply Hub | REST | JSON, CSV | ODbL |

### Tier 6 — Legal and Regulatory

| Layer | Primary Source | API | Format | License |
|-------|---------------|-----|--------|---------|
| **Admin boundaries** | Census TIGER + OSM | REST/Overpass | Shapefile, GeoJSON | Public Domain/ODbL |
| **Environmental justice** | EPA EJScreen | API + Download | GeoJSON, CSV | Public Domain |
| **Zoning** | National Zoning Atlas + local portals | Varies | Varies | Mostly Public |
| **Environmental law** | EPA + state agencies | REST (Envirofacts) | JSON, Shapefile | Public Domain |
| **International maritime** | Marine Regions | WFS | Shapefile, GeoJSON | CC BY |

### Tier 7 — Living World

| Layer | Primary Source | API | Format | License |
|-------|---------------|-----|--------|---------|
| **Species observations** | GBIF + iNaturalist | REST | JSON, DarwinCore | CC0/CC BY |
| **Endangered species** | IUCN Red List | REST (requires key) | JSON, Shapefile | Non-commercial |
| **Cultural sites** | UNESCO + Wikidata SPARQL | XML/SPARQL | JSON, KML | Open/CC0 |
| **Economic indicators** | World Bank Open Data | REST (no auth) | JSON, CSV | CC BY 4.0 |
| **Weather/climate** | NOAA/NWS | REST (api.weather.gov) | JSON, GeoJSON | Public Domain |

---

## 3. GIS Tool Stack

### Data Preparation Pipeline
```
[Source APIs/Downloads]
        ↓
[GDAL/OGR] — format conversion (ogr2ogr, gdal_translate)
        ↓
[PostGIS] — spatial database (extends our existing Neon PostgreSQL)
        ↓
[GeoServer] — serves via WMS/WFS/WCS standard APIs
        ↓
[QGIS] — analysis, visualization, scripting (PyQGIS)
        ↓
[Export] → GeoJSON/GeoTIFF for Minecraft ingestion
```

### Collaborative Mapping
```
[Google My Maps] — community curation, field collection
        ↓
[Export KML/KMZ]
        ↓
[ogr2ogr] — convert to GeoJSON
        ↓
[PostGIS / QGIS] — enrich with API data
        ↓
[WKit plugin] — render in Minecraft
```

### Key Tools

| Tool | Role | License |
|------|------|---------|
| **QGIS** | Desktop GIS for analysis + Python scripting | GPL v2 |
| **PostGIS** | Spatial SQL database (extension for PostgreSQL/Neon) | GPL v2 |
| **GeoServer** | Serve data via OGC WMS/WFS/WCS APIs | GPL v2 |
| **GDAL/OGR** | Format conversion (the Swiss Army knife of GIS) | MIT |
| **Overpass API** | Query OpenStreetMap for any real-world feature | Free/ODbL |
| **Nominatim** | Geocoding (place names → coordinates) | Free/ODbL |
| **Google My Maps** | Collaborative community mapping interface | Google ToS |
| **Leaflet / OpenLayers** | Web map visualization | BSD-2 |
| **BlueMap / Dynmap** | Minecraft → web map rendering | MIT/Apache 2.0 |

---

## 4. Minecraft Rendering Concepts

### Block-Based Overlays
| Data Type | Minecraft Representation |
|-----------|------------------------|
| Forest canopy density | Leaf block density per area |
| Water bodies | Water blocks at correct elevation |
| Superfund/toxic sites | Stained clay + warning signs + particles |
| Mining operations | Exposed ore blocks + quarry structures |
| Indigenous territory boundaries | Colored glass pane borders + info signs |
| Treaty areas | Subtle wool line borders |
| Pipelines | Iron bar lines across terrain |
| Power lines | Fence posts on towers |
| Data centers | Redstone lamp structures |
| E-waste flows | Minecart rail routes with item indicators |
| Endangered species ranges | Custom mob spawning rules |
| Economic indicators | Block material quality (gold vs cobblestone) |

### HUD/Info Displays
When a player stands at any location, WKit can show:
- **Geographic:** Lat/lng, elevation, watershed name
- **Territorial:** Indigenous nation(s), treaty(s), jurisdiction(s)
- **Environmental:** Nearest Superfund site, TRI facility, air quality
- **Legal:** Zoning, protected status, land ownership
- **Supply chain:** Nearest mine, smelter, fab, recycling facility
- **Biodiversity:** Recent species observations, IUCN status

---

## 5. Google My Maps ↔ WKit ↔ Towny Integration

### The Vision: Community-Driven Governance Mapping

Players and community members use **Google My Maps** to collaboratively mark:
- Community assets (gardens, water sources, gathering places)
- Concerns (pollution, abandoned lots, dangerous intersections)
- Proposals (where a new park should go, transit routes, food forests)
- Traditional knowledge sites (with appropriate permissions and cultural protocols)

These are **exported as KML**, converted to GeoJSON, and ingested by WKit as a "community layer" visible in the Minecraft server. Combined with a **Towny-style governance plugin**, territories in Minecraft correspond to real neighborhoods. Residents vote on proposals using TEK8 dice mechanics. Approved proposals get built in the server, creating a living model of participatory urban planning.

```
[Community] → [Google My Maps] → [KML export]
                                        ↓
                                  [ogr2ogr → GeoJSON]
                                        ↓
                              [WKit Community Layer]
                                        ↓
                          [Minecraft: visible proposals]
                                        ↓
                          [TEK8 governance: vote on proposals]
                                        ↓
                          [Approved: build in Minecraft]
                                        ↓
                          [Document: export back to My Maps]
```

---

## 6. TEK8 Integration: The Full Circuit

The TEK8 dice system maps to data layer interactions:

| Die | Element | Data Layer Domain | Minecraft Activity |
|-----|---------|-------------------|--------------------|
| D6 Earth | Earth | Mining, extraction, land tenure | Survey mineral sites, claim territories |
| D20 Water | Water | Watersheds, water quality, hydrology | Monitor water flow, restore wetlands |
| D4 Fire | Fire | Energy, data centers, crypto mining | Audit energy consumption, track heat signatures |
| D8 Air | Air | Air quality, broadband connectivity, supply chains | Scout trade routes, measure emissions |
| D12 Ether | Ether | Cultural sites, TEK, indigenous knowledge | Identify sacred sites, record oral histories |
| D10 Chaos | Chaos | E-waste, planned obsolescence, disruption | Track waste flows, expose hidden externalities |
| D100 Order | Order | Law, treaties, governance, regulations | Document jurisdictions, draft governance proposals |
| D2 Coin | Coin | Economics, trade, financial flows | Manage resources, conduct fair trade |

### The Supply Chain Meditation

The server becomes a place to **trace the full lifecycle** of the materials that make computing possible:

1. **Extraction** — Coltan mines in DRC, lithium flats in Chile, rare earths in China (USGS MRDS, conflict minerals data)
2. **Smelting** — Facilities worldwide (RMI smelter list, Open Supply Hub)
3. **Fabrication** — Semiconductor fabs in Taiwan, South Korea, US (CHIPS Act data)
4. **Assembly** — Foxconn and others (Open Supply Hub)
5. **Distribution** — Shipping routes (submarine cables, trade data)
6. **Consumption** — Data centers, AI farms, crypto mines, personal devices (EIA, CBECI)
7. **Disposal** — E-waste exports to Global South (BAN GPS data, Basel Convention)
8. **Afterlife** — Landfills, ocean dumps, recycling operations (EPA LMOP, Earth911)

Each stage is a **place on the map**, visible in Minecraft, connected by trade routes rendered as rail lines. Players can "follow" a mineral from ground to device to landfill, understanding the full circuit.

---

## 7. Arnis Upstream Improvements

For ideal integration with our live system, Arnis would benefit from:

### 7.1 Modern NBT Format
Update `fastnbt` serialization to produce post-1.18 chunk format:
- Remove "Level" wrapper compound
- Add `DataVersion`, `Status`, `Heightmaps` tags
- Use correct section Y-indexing (-4 to 19)

### 7.2 Library Mode API
Expose a function that generates blocks for a single chunk:
```rust
pub fn generate_chunk(bbox: LLBBox, chunk_x: i32, chunk_z: i32) -> ChunkData
```
This would allow direct integration without file I/O.

### 7.3 Data Layer Support
Accept supplementary GeoJSON layers as input:
```
--layers=forests.geojson,water.geojson,boundaries.geojson
```
Each layer would add blocks/markers to the generated terrain.

### 7.4 Custom Block Mapping
Allow configuration of which blocks represent which OSM tags:
```toml
[block_mapping]
"landuse=forest" = "minecraft:oak_leaves"
"natural=water" = "minecraft:water"
"man_made=data_center" = "minecraft:redstone_lamp"
```

---

## 8. References

All citations in Zotero Group Library 6420794 (tags: `mc-server`, `data-layers`).

### Datasets
1. Global Forest Watch — globalforestwatch.org
2. USGS NWIS — waterservices.usgs.gov
3. USGS WBD — usgs.gov/national-hydrography
4. NASA FIRMS — firms.modaps.eosdis.nasa.gov
5. EPA Superfund / TRI / LMOP — epa.gov
6. USGS MRDS — mrdata.usgs.gov
7. Global Energy Monitor — globalenergymonitor.org
8. Native Land Digital — native-land.ca
9. LandMark (WRI) — landmarkmap.org
10. USGS PAD-US — usgs.gov/pad-us
11. WDPA / Protected Planet — protectedplanet.net
12. Marine Regions — marineregions.org
13. GBIF — gbif.org
14. iNaturalist — inaturalist.org
15. IUCN Red List — iucnredlist.org
16. Cambridge CBECI — ccaf.io/cbnsi/cbeci
17. Open Supply Hub — opensupplyhub.org
18. iFixit API — ifixit.com/api
19. Global E-waste Monitor — ewastemonitor.info
20. SEC EDGAR Conflict Minerals — sec.gov/edgar

### Tools
21. QGIS — qgis.org
22. PostGIS — postgis.net
23. GeoServer — geoserver.org
24. GDAL/OGR — gdal.org
25. Overpass API — overpass-api.de
26. Google My Maps — google.com/maps/d/
27. BlueMap — github.com/BlueMap-Minecraft/BlueMap
28. Dynmap — github.com/webbukkit/dynmap
29. FastAsyncWorldEdit — github.com/IntellectualSites/FastAsyncWorldEdit
30. Build The Earth / Terra++ — buildtheearth.net

---

## 9. Acknowledgments

This work flows from the understanding that **every digital tool carries the weight of its material origins** — from the mountains mined for rare earths to the rivers polluted by refineries to the communities displaced by data centers. Mount Tahoma, the Mother of Waters, stands at the center of our world as a reminder that the land remembers what we take from it.

Grounded in Ivan Illich's *Tools for Conviviality*, the TEK8 methodology, and partnership with Traditional Ecological Knowledges, this project seeks to make visible the invisible circuits of extraction and disposal that sustain the digital world — so that the children who inherit this Earth can see clearly, choose wisely, and build regeneratively.

---

*Working paper. For publication at tek8.org/research. Updates tracked in Zotero Group Library 6420794.*
