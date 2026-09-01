package fun.eightxm.wkit.generator;

import fun.eightxm.wkit.sector.GeoProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ElevationCache with mock TileFetcher.
 * No HTTP calls — all tile data is synthetic.
 */
class ElevationCacheTest {

    private static final double TAHOMA_LAT = 46.8523;
    private static final double TAHOMA_LNG = -121.7603;

    @TempDir
    Path tempDir;

    private GeoProjection projection;

    @BeforeEach
    void setUp() {
        projection = new GeoProjection(TAHOMA_LAT, TAHOMA_LNG, 1.0);
    }

    // === Terrarium Decode Formula ===

    @Test
    void terrariumDecodeFormula() {
        // Terrarium encoding: height = (R * 256 + G + B / 256) - 32768
        // Sea level (0m): R=128, G=0, B=0 → (128*256 + 0 + 0) - 32768 = 0
        int r = 128, g = 0, b = 0;
        float height = (r * 256.0f + g + b / 256.0f) - 32768.0f;
        assertEquals(0.0f, height, 0.01f, "R=128,G=0,B=0 should be sea level");

        // Mount Tahoma summit (4392m): R=145, G=56, B=0 → (145*256 + 56) - 32768 = 4408
        // (approximate — real tile data may differ slightly)
        r = 145; g = 56; b = 0;
        height = (r * 256.0f + g + b / 256.0f) - 32768.0f;
        assertTrue(height > 4300 && height < 4500,
            "Should be near Tahoma summit elevation, got " + height);
    }

    // === Mock TileFetcher ===

    @Test
    void getElevationWithMockFetcher() {
        // Create a mock tile fetcher that returns a flat grid at 100m elevation
        TileFetcher mockFetcher = (tileX, tileY) -> {
            float[][] grid = new float[ElevationCache.TILE_SIZE][ElevationCache.TILE_SIZE];
            for (int y = 0; y < ElevationCache.TILE_SIZE; y++) {
                for (int x = 0; x < ElevationCache.TILE_SIZE; x++) {
                    grid[y][x] = 100.0f; // 100m everywhere
                }
            }
            return grid;
        };

        ElevationCache cache = new ElevationCache(tempDir, projection, Logger.getLogger("test"), mockFetcher);
        float elevation = cache.getElevation(TAHOMA_LAT, TAHOMA_LNG);
        assertEquals(100.0f, elevation, 0.01f, "Mock fetcher should return 100m");
    }

    @Test
    void getElevationAtMCDelegatesToProjection() {
        TileFetcher mockFetcher = (tileX, tileY) -> {
            float[][] grid = new float[ElevationCache.TILE_SIZE][ElevationCache.TILE_SIZE];
            for (int y = 0; y < ElevationCache.TILE_SIZE; y++) {
                for (int x = 0; x < ElevationCache.TILE_SIZE; x++) {
                    grid[y][x] = 50.0f;
                }
            }
            return grid;
        };

        ElevationCache cache = new ElevationCache(tempDir, projection, Logger.getLogger("test"), mockFetcher);
        float elevation = cache.getElevationAtMC(0, 0); // origin = Tahoma
        assertEquals(50.0f, elevation, 0.01f);
    }

    @Test
    void getElevationReturnsZeroWhenFetcherReturnsNull() {
        TileFetcher nullFetcher = (tileX, tileY) -> null;

        ElevationCache cache = new ElevationCache(tempDir, projection, Logger.getLogger("test"), nullFetcher);
        float elevation = cache.getElevation(TAHOMA_LAT, TAHOMA_LNG);
        assertEquals(0.0f, elevation, 0.01f, "Should return 0 when tile is null");
    }

    // === Tile Math ===

    @Test
    void tileMathAtTahomaZoom12() {
        int tileX = ElevationCache.lonToTileX(TAHOMA_LNG, ElevationCache.ZOOM);
        int tileY = ElevationCache.latToTileY(TAHOMA_LAT, ElevationCache.ZOOM);

        // At zoom 12, Tahoma (-121.76°) should be in roughly tile X=655, Y=1467
        assertTrue(tileX > 600 && tileX < 700,
            "Tahoma tile X at zoom 12 should be ~655, got " + tileX);
        assertTrue(tileY > 1400 && tileY < 1500,
            "Tahoma tile Y at zoom 12 should be ~1467, got " + tileY);
    }

    @Test
    void tileMathSymmetric() {
        // Two points in the same tile should give the same tile coords
        int tileX1 = ElevationCache.lonToTileX(TAHOMA_LNG, ElevationCache.ZOOM);
        int tileX2 = ElevationCache.lonToTileX(TAHOMA_LNG + 0.001, ElevationCache.ZOOM);
        assertEquals(tileX1, tileX2, "Very close points should be in the same tile");
    }

    // === elevationToY (legacy method on ElevationCache) ===

    @Test
    void elevationToYLinearScaling() {
        ElevationCache cache = new ElevationCache(tempDir, projection, Logger.getLogger("test"));
        // Range 0-366 (fits exactly in available range), scale=1.0
        int y = cache.elevationToY(100f, 0f, 366f);
        assertEquals(-62 + 100, y, "100m with 1:1 scale should be groundLevel + 100");
    }

    @Test
    void elevationToYCompressedRange() {
        ElevationCache cache = new ElevationCache(tempDir, projection, Logger.getLogger("test"));
        // Range 0-4392 (Tahoma range, must compress), scale = 366/4392 ≈ 0.083
        int y = cache.elevationToY(4392f, 0f, 4392f);
        assertEquals(304, y, "Max elevation should map to maxY=304");
    }

    // === In-memory Cache ===

    @Test
    void tileFetcherCalledOncePerTile() {
        int[] callCount = {0};
        TileFetcher countingFetcher = (tileX, tileY) -> {
            callCount[0]++;
            float[][] grid = new float[ElevationCache.TILE_SIZE][ElevationCache.TILE_SIZE];
            for (int y = 0; y < ElevationCache.TILE_SIZE; y++)
                for (int x = 0; x < ElevationCache.TILE_SIZE; x++)
                    grid[y][x] = 42.0f;
            return grid;
        };

        ElevationCache cache = new ElevationCache(tempDir, projection, Logger.getLogger("test"), countingFetcher);

        // Two calls for same tile should only invoke fetcher once
        cache.getElevation(TAHOMA_LAT, TAHOMA_LNG);
        cache.getElevation(TAHOMA_LAT, TAHOMA_LNG);

        assertEquals(1, callCount[0], "Fetcher should be called only once per tile (cached)");
    }
}
