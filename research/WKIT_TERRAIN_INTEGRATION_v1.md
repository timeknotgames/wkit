# Live Real-World Terrain Integration in Minecraft Java Servers: Methods, Challenges, and Proposed Architecture

**Authors:** JaiRuviel (7ABCs.com / tek8.org), Claude Code (Anthropic)
**Date:** 2026-03-18
**Version:** 1.0 — Working Paper
**Project:** WKit (Worlding Kit) — Arnis Integration for Educational Minecraft Server Networks

---

## Abstract

This paper documents our investigation into seamlessly integrating real-world geographic data into live Minecraft Java Edition servers using the Arnis world generator and Paper server software. We identify three approaches — region file hot-swapping, the ChunkGenerator API, and async block placement via FAWE — and analyze why our initial approach (direct .mca file injection) fails due to fundamental format incompatibilities between Arnis's output and Paper 1.21.8's chunk loading system. We propose a hybrid architecture using Paper's ChunkGenerator API as the correct path forward for live, seamless, on-demand terrain generation from OpenStreetMap and elevation data.

---

## 1. Introduction

### 1.1 Motivation

The 7ABCs.com and tek8.org initiative seeks to create Minecraft server networks as flagship online learning environments where children engage with urban planning, local-global economics, education, and governance. Real-world geography — generated at near 1:1 scale from OpenStreetMap (OSM) data and elevation models — serves as the foundation for place-based learning grounded in Traditional Ecological Knowledges (TEK).

### 1.2 The Tool: Arnis

Arnis (v2.5.0) is an open-source Rust application that generates Minecraft Java Edition worlds from real-world data:
- **Input:** Geographic bounding box (lat/lng) → fetches OSM data via Overpass API + elevation tiles from AWS Terrarium
- **Output:** Complete Minecraft world directory (region/*.mca files, level.dat, metadata.json)
- **Scale:** 1 block ≈ 1 meter at default scale
- **Limitation:** Always creates fresh worlds; cannot append to existing ones

### 1.3 The Challenge

We need Arnis-generated terrain to appear seamlessly in a live, multiplayer Paper server — ideally generated on-demand as players explore, with no server restarts, no visible loading boundaries, and correct geographic positioning relative to a fixed world origin (Mount Tahoma / Mount Rainier at MC coordinates 0,0).

---

## 2. Approach 1: Region File Hot-Swapping (Failed)

### 2.1 Method

Our initial approach attempted to:
1. Run Arnis CLI to generate a world in a temp directory
2. Copy .mca region files from the temp world into the live server's `world/region/` directory
3. Patch chunk NBT coordinates (xPos/zPos) to match the target geographic position
4. Force-unload and reload affected chunks via Bukkit API

### 2.2 Implementation

We built the WKit plugin with:
- `RegionFileManager.java` — copies .mca files, patches chunk coordinates using the Querz NBT library (v6.1)
- `GenerationTask.java` — async pipeline: Arnis CLI → region relocation → chunk unload/reload
- `ProceduralGenerator.java` — PlayerMoveEvent listener triggering on-demand generation

### 2.3 Why It Failed

**Root Cause 1: NBT Format Mismatch**

Arnis uses the `fastnbt` Rust library to serialize chunk data, producing NBT structures that wrap chunk data in a `"Level"` compound tag:

```
Root → "" → "Level" → { xPos, zPos, sections, isLightOn, ... }
```

Paper 1.21.8 (Minecraft 1.21.x) expects the **post-1.18 flat format** without the "Level" wrapper:

```
Root → "" → { xPos, zPos, DataVersion, Status, sections, Heightmaps, ... }
```

**Root Cause 2: Missing Critical NBT Tags**

Arnis-generated chunks lack mandatory tags that Paper requires:
- `DataVersion` (IntTag) — specifies the world format version
- `Status` (StringTag) — must be `"minecraft:full"` for fully generated chunks
- `Heightmaps` (CompoundTag) — MOTION_BLOCKING, WORLD_SURFACE, etc.
- `LastUpdate` (LongTag) — chunk modification timestamp

Without these, Paper treats the chunks as corrupted or empty.

**Root Cause 3: Region Header Corruption**

Arnis uses a pre-compiled `region.template` binary (4.1MB) as the base for every .mca file. When `fastanvil` writes chunks into this template, the chunk offset table in the 8KB region header becomes misaligned. Paper's log confirms:

```
Attempting to read chunk data at [17, 28] but got chunk data for [23, 2] instead!
Corrupt regionfile header detected! Attempting regionfile recalculation
```

**Root Cause 4: Server Overwrites at Spawn**

Even when files are correctly placed, the server's world generator (flat or void) regenerates spawn-area chunks on startup, overwriting any pre-placed .mca files near coordinates (0,0).

**Root Cause 5: No True Hot-Swap API**

Paper does not provide an API to force-reload region files from disk while the server is running. `World.unloadChunk()` followed by `World.getChunkAt()` does not reliably re-read from disk — the server may serve cached empty chunks instead.

### 2.4 Conclusion

Direct .mca file injection is fundamentally incompatible with live Paper servers. The approach might work with a server restart, but even then requires fixing the NBT format issues in Arnis's output. For live, seamless generation, a different approach is needed.

---

## 3. Approach 2: Custom ChunkGenerator (Proposed — Recommended)

### 3.1 Method

Paper's Bukkit API provides `ChunkGenerator` — an interface that plugins can implement to control how chunks are generated. When any chunk needs to be created (player approaches, server tick, etc.), Paper calls the generator's methods:

```java
public class ArnisChunkGenerator extends ChunkGenerator {
    @Override
    public void generateSurface(WorldInfo worldInfo, Random random,
                                 int chunkX, int chunkZ, ChunkData chunkData) {
        // Compute geographic bbox for this chunk
        // Fetch or cache OSM + elevation data
        // Place blocks directly into chunkData
    }
}
```

### 3.2 Advantages

- **Completely seamless** — Paper calls the generator natively; no file swapping needed
- **Correct chunk format** — Paper creates the chunk NBT with all required tags; we just fill in the blocks
- **Thread-safe** — Paper's generation methods are designed for concurrent calls
- **On-demand** — chunks generate as players explore, exactly like vanilla Minecraft
- **No restart required** — the world is created once with the custom generator; it persists

### 3.3 Architecture

```
Player explores → Paper needs chunk (X, Z)
    → ArnisChunkGenerator.generateSurface(X, Z)
        → GeoProjection: chunk coords → lat/lng bbox
        → OSM Cache: check if data for this area is cached
            → If not: fetch from Overpass API (async prefetch for nearby chunks)
        → Elevation Cache: check tile cache
            → If not: fetch from AWS Terrarium
        → BlockPlacer: convert OSM elements + elevation to block placement
        → Write blocks to ChunkData
    → Paper handles NBT, lighting, heightmaps, saving
```

### 3.4 Key Technical Decisions

**Porting vs. Shelling Out:** Rather than calling the Arnis CLI binary per chunk (slow, creates full worlds), we would port Arnis's core logic into Java:
- OSM data fetching and parsing
- Coordinate transformation (Haversine projection)
- Elevation tile fetching and interpolation
- Element processing (buildings, roads, landuse → block types)
- Block placement

Alternatively, Arnis could be refactored to expose a library API that generates a single chunk's worth of blocks given a bounding box, without creating world files.

**Caching Strategy:**
- OSM data: cache per ~1km² tile (covers multiple chunks)
- Elevation tiles: already cached by Arnis in `arnis-tile-cache/`
- Processed elements: cache parsed OSM elements per sector

**Prefetching:** When a player moves, prefetch OSM/elevation data for chunks within their view distance to minimize generation stalls.

### 3.5 Challenges

- **Porting complexity:** Arnis is ~5,000 lines of Rust. Porting to Java is significant work.
- **Generation speed:** Overpass API has rate limits and latency. Caching and prefetching are essential.
- **Cross-chunk features:** Buildings and roads span multiple chunks. Need a system to handle features that cross chunk boundaries (process a larger area, store results, serve to individual chunks).
- **Elevation consistency:** The ChunkGenerator is called per-chunk, but Arnis's elevation compression is computed per-bounding-box. Need a global elevation strategy.

---

## 4. Approach 3: FAWE Async Block Placement (Viable Alternative)

### 4.1 Method

FastAsyncWorldEdit (FAWE) provides an async API for placing blocks in a live server without lag. Instead of writing .mca files, we:

1. Run Arnis to generate a world in a temp directory
2. Read the generated world using FAWE's world reading API
3. Paste the blocks from the generated world into the live server world at the correct offset
4. FAWE handles async placement, lighting updates, and chunk sending to clients

### 4.2 Advantages

- **Works with existing Arnis output** — no need to port Arnis logic to Java
- **Proven async performance** — FAWE is battle-tested for large operations
- **Correct chunk format** — blocks are placed through the Bukkit API; Paper handles serialization
- **Live operation** — no restart needed

### 4.3 Disadvantages

- **Two-step process** — still need to generate a temp world, then paste (slower than native ChunkGenerator)
- **FAWE dependency** — adds a large plugin dependency
- **Memory overhead** — loading a full generated world into memory for pasting

### 4.4 Implementation Sketch

```java
// After Arnis generates to tempDir:
ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
Clipboard clipboard = format.getReader(new FileInputStream(file)).read();

EditSession editSession = WorldEdit.getInstance()
    .newEditSession(BukkitAdapter.adapt(world));

Operations.complete(
    new ClipboardHolder(clipboard)
        .createPaste(editSession)
        .to(BlockVector3.at(offsetX, 0, offsetZ))
        .build()
);
```

---

## 5. Approach 4: Iris Dimension Engine Integration (Speculative)

### 5.1 Method

Iris is a world generation framework for Paper that supports custom terrain generators with extensive configurability. It may be possible to write an Iris "dimension pack" that sources terrain data from OSM/elevation APIs instead of procedural noise.

### 5.2 Status

Not yet investigated in depth. Iris supports custom biome providers and noise generators but may not have a direct path for external data ingestion. Requires further research.

---

## 6. Comparison of Approaches

| Criteria | Region File Swap | ChunkGenerator API | FAWE Paste | Iris Integration |
|----------|-----------------|-------------------|------------|-----------------|
| **Seamless to players** | No (restart needed) | Yes (native) | Mostly (brief delay) | Yes (native) |
| **Correct chunk format** | No (format mismatch) | Yes (Paper creates NBT) | Yes (via API) | Yes (via API) |
| **Implementation effort** | Low (done, but broken) | High (port Arnis logic) | Medium (use existing Arnis) | Unknown |
| **Live generation** | No | Yes | Yes (with delay) | Yes |
| **Performance** | N/A (doesn't work) | Best (native pipeline) | Good (async) | Good (native) |
| **Arnis compatibility** | Direct output | Needs port or API | Direct output | Needs adapter |

---

## 7. Recommended Path Forward

### Phase 1: FAWE Bridge (Quick Win)

Use FAWE to paste Arnis-generated terrain into the live world. This works with Arnis's existing output and requires no format changes. The WKit plugin orchestrates: Arnis CLI → temp world → FAWE async paste at geographic offset.

### Phase 2: Custom ChunkGenerator (Long-term)

Port Arnis's core terrain logic into a Java ChunkGenerator. This provides the seamless, native experience where terrain generates on-demand as players explore, with no visible loading or delays.

### Phase 3: Arnis Library Mode

Contribute upstream to Arnis — refactor it to expose a library API that can generate blocks for a single chunk given a bounding box, without creating world files. This could then be called from Java via JNI/JNA or a socket API.

---

## 8. Technical Appendix: Arnis NBT Structure Analysis

### Expected by Paper 1.21.8:
```nbt
CompoundTag("") {
    IntTag("DataVersion") = 4189
    IntTag("xPos") = <chunk_x>
    IntTag("zPos") = <chunk_z>
    IntTag("yPos") = -4
    StringTag("Status") = "minecraft:full"
    LongTag("LastUpdate") = <timestamp>
    CompoundTag("Heightmaps") {
        LongArrayTag("MOTION_BLOCKING") = [...]
        LongArrayTag("WORLD_SURFACE") = [...]
    }
    ListTag("sections") {
        CompoundTag {
            ByteTag("Y") = <section_y>
            CompoundTag("block_states") {
                ListTag("palette") { ... }
                LongArrayTag("data") = [...]
            }
        }
        ...
    }
}
```

### Produced by Arnis (fastnbt):
```nbt
CompoundTag("") {
    CompoundTag("Level") {          ← WRONG: "Level" wrapper is pre-1.18 format
        IntTag("xPos") = <chunk_x>
        IntTag("zPos") = <chunk_z>
        ByteTag("isLightOn") = 1
        ListTag("sections") { ... }
        ← MISSING: DataVersion, Status, Heightmaps, LastUpdate
    }
}
```

---

## 9. References

All citations available in Zotero Group Library 6420794 (tag: `mc-server`).

1. Arnis — Real-World Geography to Minecraft World Generator. https://github.com/louis-e/arnis
2. Paper MC Server API — Version 1.21.8. https://api.papermc.io/v2/projects/paper
3. Querz NBT Library v6.1. https://github.com/Querz/NBT
4. FastAsyncWorldEdit (FAWE). https://github.com/IntellectualSites/FastAsyncWorldEdit
5. Iris Dimension Engine. https://github.com/VolmitSoftware/Iris
6. NOAA Puget Sound Bathymetric DEM (30m). NOAA NCEI.
7. USGS CoNED Topobathymetric Model — Puget Sound (1m). USGS ScienceBase.
8. AWS Terrarium Elevation Tiles. AWS Open Data.
9. Build the Earth Project. Wikipedia.
10. Terra++ / Cubic Chunks. GitHub.
11. Vertically Stacked Dimensions Mod. CurseForge.
12. Paper ChunkGenerator API Documentation. https://jd.papermc.io/paper/1.21.1/org/bukkit/generator/ChunkGenerator.html
13. Bukkit Wiki — Developing a World Generator Plugin. https://bukkit.fandom.com/wiki/Developing_a_World_Generator_Plugin

---

## 10. Acknowledgments

This research is conducted under the 7ABCs.com and tek8.org initiative, grounded in Ivan Illich's *Tools for Conviviality* and the principle that modern tools must serve human autonomy and be developed in partnership with Traditional Ecological Knowledges. Mount Tahoma (Mother of Waters) serves as both the geographic and spiritual origin of this work.

---

*Working paper. For publication at tek8.org/research. Updates tracked in Zotero Group Library 6420794.*
