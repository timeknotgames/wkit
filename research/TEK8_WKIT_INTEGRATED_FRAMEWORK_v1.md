# The Living Map Engine: A TEK8 Framework for Real-World Terrain, Data Sovereignty, and Convivial Worlding in Minecraft

**Authors:** JaiRuviel / Cody Lestelle (7ABCs.com / tek8.org), Claude Code (Anthropic)
**Date:** 2026-03-18
**Version:** 1.0 — Working Paper
**For publication at:** tek8.org/research
**Companion papers:** WKIT_TERRAIN_INTEGRATION_v1.md, WKIT_DATA_LAYERS_v1.md

---

## Abstract

We present an integrated framework connecting the TEK8 Learning Lotus — an 8-element pedagogical system grounded in Traditional Ecological Knowledges, Indigenous cartographies, and Ivan Illich's convivial tools — with a live Minecraft Java server engine capable of rendering real-world geography, environmental data, supply chain circuits, territorial sovereignty, and community governance as explorable, queryable, persistent world space.

The engine, called WKit (Worlding Kit), uses Arnis (an OSM-to-Minecraft terrain generator) with FastAsyncWorldEdit for live block placement, a geographic projection centered on Mount Tahoma (Mother of Waters), and 60+ public datasets organized into 7 tiers from forests and water to supply chain disposal. The Quillverse platform (quillverse.org) provides narrative input — text, song, dialogue, quest prompts — that translate into persistent world features through TEK8's Crystal Cycle structure.

We map each of the 8 TEK8 petals to specific engine capabilities, data layers, and Minecraft activities, demonstrating that the Crystal Cycle's daily rhythm (Coin-Music-Gather-Craft-Quest-Rest-Play-Map-Yield-Close) can operate as both a pedagogical framework AND a world-generation protocol.

---

## 1. The Problem: Maps That Extract vs. Maps That Relate

Western cartography historically served territorial claim. As documented in the D100 Indigenous Cartographies paper, "maps functioned as legal instruments converting living relationships into private property parcels." The fundamental distinction: **Western maps ask "Who owns this?" while Indigenous maps ask "What is my relationship to this place?"**

Digital tools — from Google Maps to Minecraft — inherit this extractive cartographic tradition unless deliberately reoriented. A Minecraft server showing real-world terrain is not inherently educational. It becomes educational when it asks: *Whose land is this? What was extracted from it? Where did the minerals in your computer come from? Where will your e-waste end up? What did the people who lived here first know about this watershed that we've forgotten?*

The Living Map Engine is our answer: a system that renders real-world geography not as property boundaries but as **relational space** — space that carries memory, obligation, and possibility.

---

## 2. The TEK8 Foundation

### 2.1 The Eight Petals

| Die | Element | Sense | Capital | Wellness | Engine Domain |
|-----|---------|-------|---------|----------|---------------|
| D12 | Ether | Sound | Cultural | Emotional | Quillverse narrative → world features |
| D8 | Air | Touch | Natural/Living | Physical | Forests, watersheds, biodiversity data |
| D4 | Fire | Sight | Material | Occupational | Infrastructure, fabs, extraction sites |
| D20 | Water | Taste | Experiential | Environmental | Water resources, journey routes, empathy quests |
| D6 | Earth | Smell | Spiritual | Spiritual | Indigenous knowledge, TEK overlays, sacred sites |
| D10 | Chaos | Mind | Social | Social | Community mapping, governance, e-waste tracking |
| D100 | Order | Intelligence | Intellectual | Intellectual | GIS analysis, counter-cartography, data sovereignty |
| D2 | Wealth | Instinct | Financial | Financial | Supply chain economics, capital flow tracking |

### 2.2 The Crystal Cycle as World-Generation Protocol

The 10-step Crystal Cycle (Coin-Music-Gather-Craft-Quest-Rest-Play-Map-Yield-Close) operates simultaneously as:
1. A daily learning rhythm for participants
2. A world-generation workflow for the engine
3. A governance protocol for persistent world changes

| Step | Crystal Cycle | Engine Operation | World Effect |
|------|--------------|------------------|--------------|
| 1. INSERT COIN (D2) | Set intention | Select geographic area / data layer focus | Sector queued for generation |
| 2. MUSIC BEGINS (D12) | Creative warm-up | Quillverse prompt activates | Narrative/song seeds placed as world features |
| 3. GATHER (D8) | Research & collect | Fetch OSM + elevation + dataset APIs | Raw data cached in PostGIS |
| 4. CRAFT (D4) | Build & create | Arnis generates terrain + FAWE places blocks | Terrain materialized in server |
| 5. QUEST (D20) | Deep inquiry | Players explore generated terrain | Real-world features discovered, documented |
| 6. REST (D6) | Mandatory pause | Server saves state, syncs to Neon DB | World state persisted |
| 7. PLAY (D10) | No-stakes games | Governance proposals, community mapping | Proposals entered via Google My Maps |
| 8. MAP (D100) | Reflect & pattern | GIS analysis, counter-mapping, data sovereignty audit | Knowledge documented, Zotero updated |
| 9. YIELD (D2) | Harvest & recognize | Capital flow assessment across 8 forms | Achievements logged, badges distributed |
| 10. CLOSE (D12) | Gratitude | Session archived | Quest log versioned, world state snapshot |

### 2.3 Intelligence is Emergent

In TEK8, D100 (Order/Intelligence) carries no starting value. Intelligence = Karma × 100, accumulated only through experience. This encodes a core principle: **"true understanding cannot be given, only cultivated through sustained attention to the world."**

The engine mirrors this: the world starts empty (void). Terrain and data layers only appear as participants engage — exploring, generating, querying, documenting. The world's richness is a direct measure of the community's accumulated attention. Intelligence is what you build.

---

## 3. Engine Architecture

### 3.1 Technical Stack

```
┌─────────────────────────────────────────────────────────────┐
│                    QUILLVERSE (quillverse.org)                │
│   Text, song, dialogue, quest prompts → world features       │
│   Astro + React + Neon PostgreSQL + Solana                   │
└────────────────────────┬────────────────────────────────────┘
                         │ Cloudflare Workers API
┌────────────────────────▼────────────────────────────────────┐
│              MINECRAFT SERVER NETWORK                         │
│   Paper 1.21.8 + WKit Plugin + FAWE                         │
│   Mount Tahoma origin (46.8523°N, 121.7603°W) = MC (0,0)   │
│                                                              │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│   │ Sector Mgr   │  │ Procedural   │  │ Data Layer   │     │
│   │ (generation) │  │ Generator    │  │ Renderer     │     │
│   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│          │                 │                  │              │
│   ┌──────▼─────────────────▼──────────────────▼───────┐     │
│   │              FAWE Block Placement API               │     │
│   └────────────────────────────────────────────────────┘     │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    DATA INFRASTRUCTURE                        │
│                                                              │
│   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐ │
│   │ Arnis    │  │ PostGIS  │  │ GeoServer│  │ Zotero    │ │
│   │ (terrain)│  │ (Neon DB)│  │ (WMS/WFS)│  │ (citations)│ │
│   └──────────┘  └──────────┘  └──────────┘  └───────────┘ │
│                                                              │
│   ┌──────────────────────────────────────────────────────┐  │
│   │   60+ Public Datasets (7 Tiers)                       │  │
│   │   Forests · Water · Waste · Extraction · Data Centers │  │
│   │   Indigenous Territories · Supply Chains · Legal       │  │
│   └──────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 Geographic Projection

All coordinates relative to Mount Tahoma (Mother of Waters):
- Summit at MC (0,0) — the spiritual and geographic center
- Tacoma at MC (~-51900, -44600) — ~69km northwest
- Scale: 1 block ≈ 1 meter (default)
- Haversine projection with Arnis-compatible coordinate transformation

### 3.3 Live Terrain Generation (Proven)

Arnis CLI → raw NBT read via Querz → FAWE async block placement → Paper creates proper chunks.

Verified 2026-03-18: 561,327 blocks placed live, sector registered as LIVE, terrain visible to players. Procedural generation triggers automatically as players explore via PlayerMoveEvent listener.

---

## 4. Petal-Engine Mappings

### 4.1 D12 Ether / Cultural Capital → Quillverse Narrative Bridge

**The Quillverse Connection:** quillverse.org serves as the narrative engine. Text entries, song lyrics, dialogue trees, and quest prompts written through Quillverse interfaces translate into persistent world features:

- **Songs** → placed as jukeboxes/note blocks at geographic locations, playable by visitors
- **Stories** → written on book items placed in library structures at relevant locations
- **Quest prompts** → activate when players enter specific areas, drawing from Quillverse dialogue engine
- **Character dialogue** → NPCs at landmarks speak lines authored through Quillverse

**Mechanism:** Quillverse publishes entries to a Cloudflare Worker API endpoint. WKit plugin polls or receives webhooks, places corresponding blocks/entities at the geographic coordinates specified in the entry metadata.

**Crystal Cycle mapping:** Steps 2 (MUSIC BEGINS) and 10 (CLOSE) — the bookend moments of creative expression and gratitude.

### 4.2 D8 Air / Natural Capital → Environmental Data Layers

**Datasets:** Global Forest Watch (tree cover), USGS NWIS (water flow), USGS WBD (watersheds), NASA FIRMS (fires), GBIF/iNaturalist (species), NLCD (land cover)

**Minecraft rendering:**
- Tree canopy density → leaf/log block density
- Water bodies → water blocks at correct elevation with flow direction
- Active fires → fire blocks + smoke particles (near-real-time from NASA FIRMS)
- Species observations → custom mob spawning rules by location
- Watershed boundaries → subtle colored glass borders

**Crystal Cycle mapping:** Step 3 (GATHER) — research and resource collection through direct sensory engagement with the living world.

### 4.3 D4 Fire / Material Capital → Infrastructure & Extraction

**Datasets:** Open Infrastructure Map (power lines, pipelines), USGS MRDS (mining), EIA (energy), semiconductor fab locations, Global Energy Monitor (coal/oil/gas)

**Minecraft rendering:**
- Power lines → fence posts on towers with redstone
- Mines → exposed ore blocks + quarry structures
- Pipelines → iron bar lines across terrain
- Semiconductor fabs → landmark structures
- Data centers → redstone lamp arrays

**Crystal Cycle mapping:** Step 4 (CRAFT) — building, making, precision under constraint. Every infrastructure element is a craft decision with ethical implications.

### 4.4 D20 Water / Experiential Capital → Journey & Empathy

**Datasets:** USGS water data (real-time streamflow), NOAA weather, submarine cable routes, shipping lanes

**Minecraft rendering:**
- Real-time water levels from USGS gauges → dynamic water block heights
- Storm systems → weather effects
- Journey routes (Tribal Canoe Journeys, trade routes) → marked paths across water
- Submarine cables → ocean floor glass/wool lines between continents

**Crystal Cycle mapping:** Step 5 (QUEST) — the main adventure, navigating real challenges, tasting another's experience.

### 4.5 D6 Earth / Spiritual Capital → TEK & Sacred Sites

**Datasets:** Indigenous knowledge (community-curated, with OCAP/CARE protocols), sacred site databases (by permission only), medicinal plant data (with cultural protocols)

**Minecraft rendering:**
- Sacred sites → marked only with community permission, using culturally appropriate symbols
- Medicinal plant locations → custom flower/crop blocks (with knowledge attribution)
- Seasonal observations → phenology wheel data as changing block palettes

**Critical principle: Not all knowledge should be mapped.** The engine MUST support knowledge governance — communities decide what gets documented, who accesses it, how it's stored. This is the D100/D6 bridge: Intelligence (mapping) must serve Spiritual purpose (respect).

**Crystal Cycle mapping:** Step 6 (REST) — mandatory pause, stillness, grounding. The engine rests too — some data is deliberately NOT fetched, NOT rendered, NOT cached. Rest is the work.

### 4.6 D10 Chaos / Social Capital → Community Governance

**Datasets:** Google My Maps (community curation), Wikidata cultural sites, local open data portals, zoning data

**Minecraft rendering:**
- Community proposals (from My Maps) → visible floating text/markers in game
- Governance zones → Towny-style territory system with TEK8 dice-based voting
- Social gathering spaces → automatically generated from OSM amenity data

**The Towny-TEK8 Bridge:**
Minecraft territories correspond to real neighborhoods. Residents vote on proposals using TEK8 dice mechanics — each petal contributes different voting weight. A proposal affecting water (D20) weighs Water petal engagement higher. This prevents single-dimension dominance: you can't vote on environmental policy if you've never engaged with the Earth (D6) or Water (D20) petals.

**Crystal Cycle mapping:** Step 7 (PLAY) — no-stakes games, governance experiments, creative competitions. Play is where social bonds form and community self-organizes.

### 4.7 D100 Order / Intellectual Capital → Counter-Cartography & Data Sovereignty

**Datasets:** Native Land Digital (territories, treaties, languages), Census TIGER (jurisdictions), USGS PAD-US (land ownership), Marine Regions (maritime zones), WDPA (protected areas)

**Minecraft rendering:**
- Indigenous territory boundaries → colored glass pane borders with info signs
- Treaty areas → subtle wool line borders
- Jurisdiction layers → toggleable HUD showing current legal context
- Data sovereignty audit → when players query a location, show who controls the data about that place

**OCAP/CARE in the engine:**
The engine implements data sovereignty as a first-class feature:
- **Ownership:** Communities control what data from their territories enters the system
- **Control:** API access to community data requires community-issued tokens
- **Access:** Communities can access all data about their territories regardless of who collected it
- **Possession:** Community data stored in community-controlled databases, not just our PostGIS

**Crystal Cycle mapping:** Step 8 (MAP) — reflection and pattern synthesis. "What did you learn today?" The MAP step is where Intelligence accumulates — where attention to the world becomes documented knowledge.

### 4.8 D2 Wealth / Financial Capital → Supply Chain Circuit

**Datasets:** SEC EDGAR (conflict minerals), RMI smelter list, Open Supply Hub, Global E-waste Monitor, iFixit repairability, EPA RCRAInfo, EITI (extractive industries transparency)

**The Full Circuit:** Every digital device contains materials that traveled a path:

```
EXTRACTION (D6 Earth)          → mines in DRC, Chile, China
    ↓
SMELTING (D4 Fire)             → refineries worldwide
    ↓
FABRICATION (D4 Fire)          → semiconductor fabs in Taiwan, Korea, US
    ↓
ASSEMBLY (D10 Chaos)           → factories in China, Vietnam, Mexico
    ↓
DISTRIBUTION (D8 Air)          → shipping routes, warehouses
    ↓
CONSUMPTION (D2 Wealth)        → data centers, AI farms, crypto mines, your phone
    ↓
DISPOSAL (D10 Chaos)           → e-waste exports to Global South
    ↓
AFTERLIFE (D6 Earth)           → landfills, ocean dumps, recycling
    ↓
RETURN (D12 Ether)             → materials re-enter cycle (or don't)
```

In Minecraft, each stage is a **place on the map** connected by minecart rail routes. Players follow a mineral from ground to device to landfill. The engine renders the invisible visible.

**Kuleana Economics:** Rights inseparable from responsibilities. Financial capital that forgets its cultural roots becomes extraction. Financial capital that remembers becomes abundance. The engine makes this visible by showing capital flows across all 8 forms simultaneously.

**Crystal Cycle mapping:** Steps 1 (INSERT COIN) and 9 (YIELD) — intention and harvest. What do we invest attention in? What do we receive? The D2 petal bookends the cycle because wealth is both the choice to begin and the recognition of what was gained.

---

## 5. The Quillverse Bridge: Text & Song → Persistent World

### 5.1 Architecture

Quillverse.org (Astro + React + Neon PostgreSQL) already has:
- Dialogue engine with branching narratives
- Quest system with online/tabletop/hybrid modes
- NPC libraries with archetype dialogue trees
- Territory system with room presence tracking
- TEK8 scoring across all 8 petals

The bridge connects Quillverse entries to Minecraft world features:

```
[Quillverse Author writes entry]
    ↓
[Entry stored in Neon DB with metadata: type, location, petal, tags]
    ↓
[Cloudflare Worker webhook notifies WKit]
    ↓
[WKit translates entry to world feature:]
    - Story → Book item in library structure
    - Song → Jukebox/note blocks at location
    - Quest prompt → Area trigger with NPC dialogue
    - Character → NPC entity with Quillverse dialogue tree
    - Observation → Sign/floating text at coordinates
    - Proposal → Governance marker visible to Towny system
    ↓
[Feature persisted in both Minecraft world AND Neon DB]
    ↓
[Visible to all players; attributed to author]
```

### 5.2 Digital Twins & Forks

The persistent world in Minecraft is one expression of a shared reality model. The same data can manifest as:

- **Minecraft server** → explorable 3D geography
- **Quillverse web** → narrative/text-based interface
- **Unreal Engine** → high-fidelity visualization (future)
- **Google My Maps** → collaborative community mapping
- **BlueMap/Dynmap** → web-rendered 3D map overlay
- **Physical installations** → projected/printed maps from the same data

Each is a **digital twin** of the same underlying geographic and narrative data. A song entered in Quillverse appears as a jukebox in Minecraft, a pin on Google My Maps, a waypoint in Unreal Engine, and a citation in Zotero. The engine is the shared substrate; the interfaces are forks.

---

## 6. Data Sovereignty as Engine Feature

### 6.1 The Counter-Cartographic Imperative

Every dataset in the engine carries metadata about **who it serves.** Following the D100 Indigenous Cartographies paper:

- Not all knowledge should be mapped
- Documentation without consent is extraction
- Communities determine what gets recorded, who accesses it, how it's stored
- The question is not "What data exists?" but "Whose attention counts?"

### 6.2 Implementation

The engine implements OCAP/CARE as technical features:

| Principle | Engine Feature |
|-----------|---------------|
| **Collective Benefit** | Community dashboard showing how their data is used |
| **Authority to Control** | Community-issued API tokens for data access |
| **Responsibility** | Mandatory attribution on all rendered features |
| **Ethics** | Consent workflow before rendering community data |
| **Ownership** | Community-controlled PostGIS instances for their data |
| **Control** | Toggle visibility of community layers per-viewer |
| **Access** | Communities see all data about their territory |
| **Possession** | Data export in standard formats (GeoJSON, KML) on demand |

### 6.3 What The Engine Deliberately Does NOT Map

- Sacred sites without explicit community permission
- Ceremonial practices and protocols
- Medicine locations and traditional knowledge not designated for sharing
- Any data a community requests to be withheld

The void in the map is itself data — it says "here is knowledge that exists but is not ours to display." This teaches a lesson no filled-in map can: **attention must be earned through relationship.**

---

## 7. Assessment Through the Engine

Following the Scholastic Framework's formative assessment model, the engine generates evidence across all 8 petals:

| Step | Evidence | Petal(s) |
|------|----------|----------|
| INSERT COIN | Quality of intention statement | D2 |
| MUSIC BEGINS | Quillverse entry created | D12 |
| GATHER | Datasets queried, areas explored | D8 |
| CRAFT | Blocks placed, structures built | D4 |
| QUEST | Distance traveled, features discovered | D20 |
| REST | Session pause observed (server-side tracking) | D6 |
| PLAY | Governance proposals entered, community interactions | D10 |
| MAP | Zotero citations added, GIS analysis performed | D100 |
| YIELD | Capital flow self-assessment | D2 |
| CLOSE | Gratitude expressed (Quillverse close entry) | D12 |

Each player's Personal Learning Lotus fills based on their engine engagement across all 8 dimensions. The Attainment Principle ensures no petal dominates: a poet's D12 work equals a coder's D4 work if both engage at equal percentages.

---

## 8. Implementation Roadmap

### Phase 1: Terrain Engine (COMPLETE — 2026-03-18)
- [x] WKit plugin with FAWE paste pipeline (561K blocks live)
- [x] Mount Tahoma geographic projection
- [x] Procedural generation listener
- [x] Arnis library with catalog/schema
- [x] Zotero citation system (25 items)
- [x] Research papers v1 and v2

### Phase 2: Data Layers + Quillverse Bridge (Next)
- [ ] PostGIS spatial extension on Neon DB
- [ ] First data layer: forests + water (Global Forest Watch, USGS NWIS)
- [ ] Cloudflare Worker bridge between Quillverse and WKit
- [ ] Quillverse entry → Minecraft world feature pipeline
- [ ] BlueMap web map with data layer overlays
- [ ] Google My Maps → KML → GeoJSON → WKit pipeline

### Phase 3: Governance + TEK8 Scoring
- [ ] Towny-style territory system with TEK8 dice voting
- [ ] Crystal Cycle session management in WKit
- [ ] Personal Learning Lotus tracking
- [ ] Native Land Digital overlay (with OCAP/CARE protocols)
- [ ] Data sovereignty consent workflow

### Phase 4: Supply Chain Circuit
- [ ] Full extraction-to-disposal pathway rendering
- [ ] USGS MRDS mining locations
- [ ] E-waste flow visualization (BAN GPS data)
- [ ] iFixit repairability integration
- [ ] Capital flow assessment across 8 forms

### Phase 5: Vertical Terrain (tahoma-gen)
- [ ] 1:1 Mount Tahoma in vertically sliced worlds
- [ ] Bathymetry mirror (NOAA Puget Sound data)
- [ ] Geological layer generation
- [ ] Stacked dimension portal system

### Phase 6: Multi-Platform Digital Twins
- [ ] Unreal Engine fork of the same geographic data
- [ ] Physical installation outputs (maps, models)
- [ ] Mobile companion (Google My Maps + AR overlays)
- [ ] Full Velocity proxy network with specialized servers

---

## 9. The Convivial Principle

Ivan Illich wrote that convivial tools are those "which give each person who uses them the greatest opportunity to enrich the environment with the fruits of his or her vision." A Minecraft server that renders real-world geography is not automatically convivial. It becomes convivial when:

1. **It amplifies autonomy** — participants choose what to explore, generate, document
2. **Its costs are transparent** — the supply chain circuit shows the material cost of the server itself
3. **It teaches to liberate** — participants learn to read the world, not just the screen
4. **It has no credential gates** — anyone can enter, explore, contribute
5. **It centers peer relationships** — the Towny governance system, not admin fiat, shapes the world
6. **It favors repair over replacement** — the iFixit integration, the recycling layer, the afterlife tracking
7. **It operates at human scale** — the Crystal Cycle's 30-minute steps, the Lodge Keeper's 24-participant ratio
8. **It cultivates joyful sobriety** — REST is mandatory; screens go OFF; gratitude closes every session

The Living Map Engine is convivial because it gives children — who will inherit this Earth for a time — the tools to see their world as it truly is: interconnected, contested, extractable, healable, and theirs to tend.

---

## 10. References

Full Zotero library: Group 6420794 (tags: `mc-server`, `tek8-research`, `data-layers`)

### TEK8 Framework Papers (tek8.org/research)
1. TEK8 Learning Lotus: A Scholastic Framework (120 citations)
2. TEK8 Capital Flow Study (35 citations)
3. Indigenous Cartographies: Mapping, Wayfinding, Knowledge Systems — D100 (53 citations)
4. The Quillverse Knowledge Engine (12 citations)
5. TEK8 Lotus Emergence Study (15 citations)

### WKit Research Papers (this project)
6. WKit Terrain Integration v1 — Methods, Challenges, FAWE Breakthrough
7. WKit Data Layer Architecture v1 — From Terrain to Territorial Sovereignty
8. This paper

### Technical References
9-30. [See Zotero library — 25 items covering Arnis, Paper MC, FAWE, Querz NBT, GIS tools, datasets]

---

## Acknowledgments

Mount Tahoma, the Mother of Waters, stands at the center of this world — a 4,392-meter reminder that the land extends deeper than any mine and higher than any data center. The water that falls on her summit feeds the rivers that feed the Sound that feeds the ocean that feeds the clouds that return to her peak. This is the circuit we seek to make visible.

This work is indebted to the TEK8 framework's grounding in Indigenous knowledge systems — particularly the Polynesian wayfinding traditions of Mau Piailug and Nainoa Thompson, the Aboriginal songlines that encode 7,000 years of geographic memory in song, the Coast Salish canoe journey traditions of the Salish Sea, and the OCAP/CARE principles that insist knowledge governance is inseparable from knowledge creation.

We acknowledge that the minerals powering the servers running this engine were extracted from someone's homeland. The circuit is not abstract. The Living Map Engine exists to make that visible — so that the children using it can see clearly, choose wisely, and build regeneratively.

*Grounded in Ivan Illich's Tools for Conviviality. Built with the TEK8 Learning Lotus. Centered on Mount Tahoma. For the children who will inherit the Earth.*

---

*Working paper. For publication at tek8.org/research. Updates tracked in Zotero Group Library 6420794.*
*Community engagement: skool.com/7abcs*
*Codebase: github.com (pending — WKit, tahoma-gen)*
