package fun.eightxm.wkit.generator;

import fun.eightxm.wkit.sector.GeoProjection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Custom ChunkGenerator that creates real-world terrain from elevation + OSM data.
 * Paper calls this natively — every chunk generates on-demand as players explore.
 *
 * Features:
 * - Piecewise elevation mapping (0-200m at 1:1, compressed above — walkable everywhere)
 * - OSM buildings rendered as structures
 * - OSM roads rendered as path/stone blocks
 * - Water bodies filled correctly
 * - Forests rendered as trees
 * - Parks as grass + flowers
 *
 * All completely thread-safe for concurrent chunk generation.
 */
public class ArnisChunkGenerator extends ChunkGenerator {

    private final GeoProjection projection;
    private final ElevationCache elevationCache;
    private final OSMCache osmCache;
    private final Logger logger;

    public ArnisChunkGenerator(GeoProjection projection, Path cacheDir, Logger logger) {
        this.projection = projection;
        this.elevationCache = new ElevationCache(cacheDir.resolve("elevation"), projection, logger);
        this.osmCache = new OSMCache(cacheDir.resolve("osm"), logger);
        this.logger = logger;
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateBedrock() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new ArnisBiomeProvider(projection, elevationCache);
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        int minY = chunkData.getMinHeight();

        // Pre-fetch OSM data for this chunk's geographic area (async, non-blocking)
        double[] chunkMinLL = projection.toLatlng(chunkX * 16, chunkZ * 16 + 15);
        double[] chunkMaxLL = projection.toLatlng(chunkX * 16 + 15, chunkZ * 16);
        double minLat = Math.min(chunkMinLL[0], chunkMaxLL[0]);
        double maxLat = Math.max(chunkMinLL[0], chunkMaxLL[0]);
        double minLng = Math.min(chunkMinLL[1], chunkMaxLL[1]);
        double maxLng = Math.max(chunkMinLL[1], chunkMaxLL[1]);

        List<OSMCache.OSMElement> osmElements = osmCache.getElements(minLat, minLng, maxLat, maxLng);

        // Build a lookup for which blocks in this chunk are covered by OSM features
        // [x][z] -> feature type (null = natural terrain)
        OSMCache.OSMElement.Type[][] featureMap = new OSMCache.OSMElement.Type[16][16];
        String[][] featureSubtype = new String[16][16];

        for (OSMCache.OSMElement elem : osmElements) {
            markFeature(featureMap, featureSubtype, elem, chunkX, chunkZ);
        }

        // Generate terrain column by column
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                // Get real elevation
                float elevation = elevationCache.getElevationAtMC(worldX, worldZ);
                int surfaceY = ElevationMapper.elevationToY(elevation);
                int seaY = ElevationMapper.getSeaLevelY();

                // Clamp
                surfaceY = Math.max(minY + 1, Math.min(surfaceY, 319));

                // === BEDROCK ===
                chunkData.setBlock(x, minY, z, Material.BEDROCK);

                // === STONE FILL ===
                for (int y = minY + 1; y < surfaceY - 3; y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }

                // === DIRT LAYER ===
                for (int y = Math.max(minY + 1, surfaceY - 3); y < surfaceY; y++) {
                    chunkData.setBlock(x, y, z, Material.DIRT);
                }

                // === SURFACE + FEATURES ===
                OSMCache.OSMElement.Type feature = featureMap[x][z];

                if (elevation < -1) {
                    // Underwater
                    chunkData.setBlock(x, surfaceY, z, Material.SAND);
                    for (int y = surfaceY + 1; y <= seaY; y++) {
                        chunkData.setBlock(x, y, z, Material.WATER);
                    }
                } else if (feature == OSMCache.OSMElement.Type.BUILDING) {
                    // Building footprint — stone base + walls
                    chunkData.setBlock(x, surfaceY, z, Material.SMOOTH_STONE);
                    int buildingHeight = getBuildingHeight(featureSubtype[x][z]);
                    for (int y = surfaceY + 1; y <= surfaceY + buildingHeight; y++) {
                        // Walls on edges, air inside (simplified — full wall)
                        chunkData.setBlock(x, y, z, Material.STONE_BRICKS);
                    }
                    // Roof
                    chunkData.setBlock(x, surfaceY + buildingHeight + 1, z, Material.DARK_OAK_SLAB);
                } else if (feature == OSMCache.OSMElement.Type.HIGHWAY) {
                    // Road surface
                    String roadType = featureSubtype[x][z];
                    Material roadMat = getRoadMaterial(roadType);
                    chunkData.setBlock(x, surfaceY, z, roadMat);
                } else if (feature == OSMCache.OSMElement.Type.WATER || feature == OSMCache.OSMElement.Type.WATERWAY) {
                    // Water body/river
                    chunkData.setBlock(x, surfaceY - 1, z, Material.CLAY);
                    chunkData.setBlock(x, surfaceY, z, Material.WATER);
                } else if (feature == OSMCache.OSMElement.Type.FOREST) {
                    // Forest — grass with occasional trees
                    chunkData.setBlock(x, surfaceY, z, Material.GRASS_BLOCK);
                    if (random.nextInt(8) == 0) {
                        // Simple tree
                        for (int y = surfaceY + 1; y <= surfaceY + 5; y++) {
                            chunkData.setBlock(x, y, z, Material.OAK_LOG);
                        }
                        // Leaves
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                if (x + dx >= 0 && x + dx < 16 && z + dz >= 0 && z + dz < 16) {
                                    for (int dy = 3; dy <= 6; dy++) {
                                        if (Math.abs(dx) + Math.abs(dz) <= 3) {
                                            chunkData.setBlock(x + dx, surfaceY + dy, z + dz, Material.OAK_LEAVES);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (feature == OSMCache.OSMElement.Type.PARK) {
                    // Park — grass + flowers
                    chunkData.setBlock(x, surfaceY, z, Material.GRASS_BLOCK);
                    if (random.nextInt(4) == 0) {
                        Material flower = random.nextBoolean() ? Material.DANDELION : Material.POPPY;
                        chunkData.setBlock(x, surfaceY + 1, z, flower);
                    }
                } else if (feature == OSMCache.OSMElement.Type.LANDUSE) {
                    String landuse = featureSubtype[x][z];
                    chunkData.setBlock(x, surfaceY, z, getLanduseMaterial(landuse));
                } else {
                    // Natural terrain based on elevation — biome-appropriate surfaces
                    if (elevation > 4000) {
                        // Summit — packed ice / snow
                        chunkData.setBlock(x, surfaceY, z, Material.PACKED_ICE);
                        chunkData.setBlock(x, surfaceY + 1, z, Material.SNOW);
                    } else if (elevation > 2800) {
                        // Glacier / high alpine
                        chunkData.setBlock(x, surfaceY, z, random.nextInt(3) == 0 ? Material.PACKED_ICE : Material.SNOW_BLOCK);
                    } else if (elevation > 2000) {
                        // Alpine meadow / rocky
                        chunkData.setBlock(x, surfaceY, z, random.nextInt(3) == 0 ? Material.STONE : Material.SNOW_BLOCK);
                    } else if (elevation > 1200) {
                        // Subalpine forest
                        chunkData.setBlock(x, surfaceY, z, Material.PODZOL);
                        if (random.nextInt(6) == 0) {
                            placeSpruceTree(chunkData, x, surfaceY + 1, z, random);
                        }
                    } else if (elevation > 600) {
                        // Montane conifer forest (dense)
                        chunkData.setBlock(x, surfaceY, z, Material.PODZOL);
                        if (random.nextInt(4) == 0) {
                            placeSpruceTree(chunkData, x, surfaceY + 1, z, random);
                        } else if (random.nextInt(8) == 0) {
                            chunkData.setBlock(x, surfaceY + 1, z, Material.FERN);
                        }
                    } else if (elevation > 200) {
                        // Temperate rainforest (lush)
                        chunkData.setBlock(x, surfaceY, z, random.nextInt(4) == 0 ? Material.PODZOL : Material.GRASS_BLOCK);
                        if (random.nextInt(5) == 0) {
                            placeTallTree(chunkData, x, surfaceY + 1, z, random);
                        } else if (random.nextInt(6) == 0) {
                            chunkData.setBlock(x, surfaceY + 1, z,
                                random.nextBoolean() ? Material.FERN : Material.LARGE_FERN);
                        }
                    } else if (elevation > 10) {
                        // Lowland — grass with occasional trees
                        chunkData.setBlock(x, surfaceY, z, Material.GRASS_BLOCK);
                        if (random.nextInt(20) == 0) {
                            placeOakTree(chunkData, x, surfaceY + 1, z, random);
                        } else if (random.nextInt(12) == 0) {
                            chunkData.setBlock(x, surfaceY + 1, z,
                                random.nextInt(3) == 0 ? Material.SHORT_GRASS : Material.FERN);
                        }
                    } else if (elevation > 3) {
                        // Near coast — sandy grass
                        chunkData.setBlock(x, surfaceY, z, random.nextBoolean() ? Material.GRASS_BLOCK : Material.SAND);
                    } else if (elevation > -1) {
                        // Beach
                        chunkData.setBlock(x, surfaceY, z, Material.SAND);
                    } else {
                        // Below sea level — underwater
                        chunkData.setBlock(x, surfaceY, z, Material.GRAVEL);
                        for (int y = surfaceY + 1; y <= seaY; y++) {
                            chunkData.setBlock(x, y, z, Material.WATER);
                        }
                        // Kelp/seagrass
                        if (random.nextInt(4) == 0 && surfaceY + 1 < seaY) {
                            chunkData.setBlock(x, surfaceY + 1, z, Material.SEAGRASS);
                        }
                    }
                }
            }
        }
    }

    /**
     * Mark blocks in the feature map that are covered by an OSM element.
     * Uses simple point-in-polygon or line rasterization.
     */
    private void markFeature(OSMCache.OSMElement.Type[][] map, String[][] subtypes,
                              OSMCache.OSMElement elem, int chunkX, int chunkZ) {
        int chunkMinX = chunkX * 16;
        int chunkMinZ = chunkZ * 16;

        if (elem.type == OSMCache.OSMElement.Type.HIGHWAY || elem.type == OSMCache.OSMElement.Type.WATERWAY) {
            // Rasterize line segments
            int width = (elem.type == OSMCache.OSMElement.Type.HIGHWAY) ? getHighwayWidth(elem.subtype) : 2;
            for (int i = 0; i < elem.coordinates.size() - 1; i++) {
                double[] a = elem.coordinates.get(i);
                double[] b = elem.coordinates.get(i + 1);
                int[] mcA = projection.toMC(a[0], a[1]);
                int[] mcB = projection.toMC(b[0], b[1]);
                rasterizeLine(map, subtypes, mcA[0], mcA[1], mcB[0], mcB[1],
                    chunkMinX, chunkMinZ, elem.type, elem.subtype, width);
            }
        } else {
            // Rasterize polygon (simple scanline)
            rasterizePolygon(map, subtypes, elem.coordinates, chunkMinX, chunkMinZ,
                elem.type, elem.subtype);
        }
    }

    private void rasterizeLine(OSMCache.OSMElement.Type[][] map, String[][] subtypes,
                                int x0, int z0, int x1, int z1,
                                int chunkMinX, int chunkMinZ,
                                OSMCache.OSMElement.Type type, String subtype, int width) {
        // Bresenham with width
        int dx = Math.abs(x1 - x0), dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1, sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;
        int steps = Math.max(dx, dz);

        for (int i = 0; i <= Math.min(steps, 2000); i++) {
            for (int w = -width / 2; w <= width / 2; w++) {
                int lx = x0 + w - chunkMinX;
                int lz = z0 - chunkMinZ;
                if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
                    map[lx][lz] = type;
                    subtypes[lx][lz] = subtype;
                }
                lx = x0 - chunkMinX;
                lz = z0 + w - chunkMinZ;
                if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
                    map[lx][lz] = type;
                    subtypes[lx][lz] = subtype;
                }
            }

            if (x0 == x1 && z0 == z1) break;
            int e2 = 2 * err;
            if (e2 > -dz) { err -= dz; x0 += sx; }
            if (e2 < dx) { err += dx; z0 += sz; }
        }
    }

    private void rasterizePolygon(OSMCache.OSMElement.Type[][] map, String[][] subtypes,
                                   List<double[]> coords, int chunkMinX, int chunkMinZ,
                                   OSMCache.OSMElement.Type type, String subtype) {
        // Convert to MC coordinates
        int[] mcXs = new int[coords.size()];
        int[] mcZs = new int[coords.size()];
        for (int i = 0; i < coords.size(); i++) {
            int[] mc = projection.toMC(coords.get(i)[0], coords.get(i)[1]);
            mcXs[i] = mc[0];
            mcZs[i] = mc[1];
        }

        // Simple point-in-polygon test for each cell in the chunk
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int px = chunkMinX + lx;
                int pz = chunkMinZ + lz;
                if (pointInPolygon(px, pz, mcXs, mcZs)) {
                    map[lx][lz] = type;
                    subtypes[lx][lz] = subtype;
                }
            }
        }
    }

    private boolean pointInPolygon(int px, int pz, int[] polyX, int[] polyZ) {
        boolean inside = false;
        int n = polyX.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((polyZ[i] > pz) != (polyZ[j] > pz) &&
                px < (polyX[j] - polyX[i]) * (pz - polyZ[i]) / (polyZ[j] - polyZ[i]) + polyX[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    private int getBuildingHeight(String subtype) {
        if (subtype == null) return 4;
        return switch (subtype) {
            case "yes", "residential", "house" -> 4;
            case "apartments" -> 8 + (int)(Math.random() * 8);
            case "commercial", "office" -> 6 + (int)(Math.random() * 10);
            case "industrial", "warehouse" -> 5;
            case "church", "cathedral" -> 12;
            case "school", "university" -> 4;
            case "hospital" -> 6;
            case "skyscraper" -> 20 + (int)(Math.random() * 30);
            default -> 4;
        };
    }

    private int getHighwayWidth(String subtype) {
        if (subtype == null) return 2;
        return switch (subtype) {
            case "motorway", "trunk" -> 6;
            case "primary" -> 4;
            case "secondary", "tertiary" -> 3;
            case "residential", "service" -> 2;
            case "footway", "path", "cycleway" -> 1;
            default -> 2;
        };
    }

    private Material getRoadMaterial(String subtype) {
        if (subtype == null) return Material.GRAY_CONCRETE;
        return switch (subtype) {
            case "motorway", "trunk", "primary" -> Material.BLACK_CONCRETE;
            case "secondary", "tertiary" -> Material.GRAY_CONCRETE;
            case "residential" -> Material.GRAY_CONCRETE;
            case "footway", "path" -> Material.GRAVEL;
            case "cycleway" -> Material.GREEN_CONCRETE;
            case "track" -> Material.DIRT_PATH;
            default -> Material.GRAY_CONCRETE;
        };
    }

    private Material getLanduseMaterial(String landuse) {
        if (landuse == null) return Material.GRASS_BLOCK;
        return switch (landuse) {
            case "residential" -> Material.GRASS_BLOCK;
            case "commercial", "retail" -> Material.SMOOTH_STONE;
            case "industrial" -> Material.GRAY_CONCRETE;
            case "farmland", "farm" -> Material.FARMLAND;
            case "forest" -> Material.PODZOL;
            case "meadow", "grass" -> Material.GRASS_BLOCK;
            case "cemetery" -> Material.SOUL_SAND;
            case "railway" -> Material.IRON_BLOCK;
            case "military" -> Material.RED_CONCRETE;
            default -> Material.GRASS_BLOCK;
        };
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        float elev = elevationCache.getElevationAtMC(0, 0);
        int y = ElevationMapper.elevationToY(elev) + 2;
        return new Location(world, 0.5, Math.max(y, -10), 0.5);
    }

    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, org.bukkit.HeightMap heightMap) {
        float elev = elevationCache.getElevationAtMC(x, z);
        return ElevationMapper.elevationToY(elev);
    }

    // === Tree placement helpers (stay within chunk bounds) ===

    private void placeSpruceTree(ChunkData data, int x, int baseY, int z, Random random) {
        int height = 5 + random.nextInt(4);
        for (int y = 0; y < height; y++) {
            if (baseY + y > 319) break;
            data.setBlock(x, baseY + y, z, Material.SPRUCE_LOG);
        }
        // Conical leaves
        for (int y = 2; y <= height + 1; y++) {
            int radius = Math.max(0, (height - y + 2) / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int lx = x + dx, lz = z + dz;
                    if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && baseY + y <= 319) {
                        if (dx == 0 && dz == 0) continue; // trunk
                        if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                            data.setBlock(lx, baseY + y, lz, Material.SPRUCE_LEAVES);
                        }
                    }
                }
            }
        }
    }

    private void placeTallTree(ChunkData data, int x, int baseY, int z, Random random) {
        // Pacific NW tall tree — dark oak or spruce, tall trunk
        int height = 7 + random.nextInt(5);
        Material log = random.nextBoolean() ? Material.DARK_OAK_LOG : Material.SPRUCE_LOG;
        Material leaf = random.nextBoolean() ? Material.DARK_OAK_LEAVES : Material.SPRUCE_LEAVES;
        for (int y = 0; y < height; y++) {
            if (baseY + y > 319) break;
            data.setBlock(x, baseY + y, z, log);
        }
        for (int y = height - 3; y <= height + 1; y++) {
            int radius = y == height + 1 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int lx = x + dx, lz = z + dz;
                    if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && baseY + y <= 319) {
                        if (dx == 0 && dz == 0 && y < height) continue;
                        data.setBlock(lx, baseY + y, lz, leaf);
                    }
                }
            }
        }
    }

    private void placeOakTree(ChunkData data, int x, int baseY, int z, Random random) {
        int height = 4 + random.nextInt(3);
        for (int y = 0; y < height; y++) {
            if (baseY + y > 319) break;
            data.setBlock(x, baseY + y, z, Material.OAK_LOG);
        }
        for (int y = height - 2; y <= height + 1; y++) {
            int radius = y >= height ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int lx = x + dx, lz = z + dz;
                    if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && baseY + y <= 319) {
                        if (dx == 0 && dz == 0 && y < height) continue;
                        if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                            data.setBlock(lx, baseY + y, lz, Material.OAK_LEAVES);
                        }
                    }
                }
            }
        }
    }

    public GeoProjection getProjection() { return projection; }
    public ElevationCache getElevationCache() { return elevationCache; }
    public OSMCache getOsmCache() { return osmCache; }
}
