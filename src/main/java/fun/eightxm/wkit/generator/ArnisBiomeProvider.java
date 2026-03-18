package fun.eightxm.wkit.generator;

import fun.eightxm.wkit.sector.GeoProjection;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

/**
 * Assigns real-world-appropriate biomes based on elevation and geographic location.
 * Uses elevation data + latitude to determine climate zone, then maps to MC biomes.
 *
 * Pacific Northwest (Mount Tahoma / Tacoma region):
 * - Sea level coast: BEACH / OCEAN
 * - Lowland urban: PLAINS
 * - Lowland forest: OLD_GROWTH_SPRUCE_TAIGA (temperate rainforest)
 * - Mid elevation: TAIGA (conifer forest)
 * - Subalpine: WINDSWEPT_FOREST
 * - Alpine: SNOWY_SLOPES
 * - Summit: FROZEN_PEAKS / ICE_SPIKES
 * - Water: RIVER / OCEAN / DEEP_OCEAN
 */
public class ArnisBiomeProvider extends BiomeProvider {

    private final GeoProjection projection;
    private final ElevationCache elevationCache;

    public ArnisBiomeProvider() {
        this.projection = null;
        this.elevationCache = null;
    }

    public ArnisBiomeProvider(GeoProjection projection, ElevationCache elevationCache) {
        this.projection = projection;
        this.elevationCache = elevationCache;
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        if (projection == null || elevationCache == null) {
            return Biome.PLAINS;
        }

        float elevation = elevationCache.getElevationAtMC(x, z);
        int surfaceY = ElevationMapper.elevationToY(elevation);

        // If this Y is well below the surface, underground biome
        if (y < surfaceY - 20) {
            return Biome.DRIPSTONE_CAVES;
        }

        // Underwater areas
        if (elevation < -5) {
            if (elevation < -50) return Biome.DEEP_OCEAN;
            return Biome.OCEAN;
        }

        // Coastal
        if (elevation >= -5 && elevation < 3) {
            return Biome.BEACH;
        }

        // River level (check if near water features — simplified: use elevation bands)
        if (elevation >= 3 && elevation < 10) {
            return Biome.SWAMP; // wetlands near sea level
        }

        // Lowland (0-200m) — Pacific Northwest temperate zone
        if (elevation < 200) {
            return Biome.PLAINS; // urban areas, farms
        }

        // Low forest (200-600m) — temperate rainforest
        if (elevation < 600) {
            return Biome.OLD_GROWTH_SPRUCE_TAIGA;
        }

        // Mid forest (600-1200m) — montane conifer forest
        if (elevation < 1200) {
            return Biome.TAIGA;
        }

        // Subalpine (1200-2000m)
        if (elevation < 2000) {
            return Biome.WINDSWEPT_FOREST;
        }

        // Alpine meadow (2000-2800m)
        if (elevation < 2800) {
            return Biome.SNOWY_SLOPES;
        }

        // High alpine / glacier (2800-4000m)
        if (elevation < 4000) {
            return Biome.FROZEN_PEAKS;
        }

        // Summit zone (4000m+)
        return Biome.ICE_SPIKES;
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return List.of(
            Biome.PLAINS, Biome.OLD_GROWTH_SPRUCE_TAIGA, Biome.TAIGA,
            Biome.WINDSWEPT_FOREST, Biome.SNOWY_SLOPES, Biome.FROZEN_PEAKS,
            Biome.ICE_SPIKES, Biome.OCEAN, Biome.DEEP_OCEAN, Biome.BEACH,
            Biome.SWAMP, Biome.RIVER, Biome.DRIPSTONE_CAVES
        );
    }
}
