package fun.eightxm.wkit.tools;

import fun.eightxm.wkit.sector.GeoProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CoordsTool — coordinate conversion and distance utilities.
 */
class CoordsToolTest {

    private static final double TAHOMA_LAT = 46.8523;
    private static final double TAHOMA_LNG = -121.7603;

    private CoordsTool tool;

    @BeforeEach
    void setUp() {
        GeoProjection projection = new GeoProjection(TAHOMA_LAT, TAHOMA_LNG, 1.0);
        tool = new CoordsTool(projection);
    }

    // === getLatLng ===

    @Test
    void getLatLngAtOrigin() {
        double[] ll = tool.getLatLng(0, 0);
        assertEquals(TAHOMA_LAT, ll[0], 0.0001);
        assertEquals(TAHOMA_LNG, ll[1], 0.0001);
    }

    @Test
    void getLatLngAwayFromOrigin() {
        // Some positive offset
        double[] ll = tool.getLatLng(1000, 1000);
        // X=1000 east → lng should be more positive
        assertTrue(ll[1] > TAHOMA_LNG, "1000 blocks east should increase lng");
        // Z=1000 south → lat should decrease
        assertTrue(ll[0] < TAHOMA_LAT, "1000 blocks south should decrease lat");
    }

    // === distanceBlocks ===

    @Test
    void distanceBlocksSamePointIsZero() {
        assertEquals(0.0, tool.distanceBlocks(5, 5, 5, 5), 0.001);
    }

    @Test
    void distanceBlocksHorizontal() {
        assertEquals(100.0, tool.distanceBlocks(0, 0, 100, 0), 0.001);
    }

    @Test
    void distanceBlocksVertical() {
        assertEquals(100.0, tool.distanceBlocks(0, 0, 0, 100), 0.001);
    }

    @Test
    void distanceBlocksDiagonal() {
        // 3-4-5 triangle
        assertEquals(5.0, tool.distanceBlocks(0, 0, 3, 4), 0.001);
    }

    @Test
    void distanceBlocksNegativeCoords() {
        // Should work with negative coords too
        assertEquals(5.0, tool.distanceBlocks(-3, -4, 0, 0), 0.001);
    }

    // === distanceMeters ===

    @Test
    void distanceMetersOriginToOriginIsZero() {
        double d = tool.distanceMeters(0, 0, 0, 0);
        assertEquals(0.0, d, 0.1);
    }

    @Test
    void distanceMetersApproximatesBlockDistance() {
        // At scale 1.0, 1 block ≈ 1 meter, so 1000 blocks east should be ~1000m
        double d = tool.distanceMeters(0, 0, 1000, 0);
        assertTrue(d > 900 && d < 1100,
            "1000 blocks at scale 1.0 should be roughly 1000m, got " + d);
    }
}
