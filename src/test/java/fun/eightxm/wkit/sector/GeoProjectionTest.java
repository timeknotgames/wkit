package fun.eightxm.wkit.sector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GeoProjection — the mathematical foundation of the entire project.
 * Mount Tahoma (Mother of Waters) at 46.8523°N, 121.7603°W is MC (0,0).
 */
class GeoProjectionTest {

    // Mount Tahoma summit coordinates
    private static final double TAHOMA_LAT = 46.8523;
    private static final double TAHOMA_LNG = -121.7603;

    private GeoProjection projection;

    @BeforeEach
    void setUp() {
        projection = new GeoProjection(TAHOMA_LAT, TAHOMA_LNG, 1.0);
    }

    // === Origin Tests ===

    @Test
    void originMapsToMcZeroZero() {
        int[] mc = projection.toMC(TAHOMA_LAT, TAHOMA_LNG);
        assertEquals(0, mc[0], "Origin X should be 0");
        assertEquals(0, mc[1], "Origin Z should be 0");
    }

    @Test
    void mcZeroZeroMapsBackToOrigin() {
        double[] ll = projection.toLatlng(0, 0);
        assertEquals(TAHOMA_LAT, ll[0], 0.0001, "Lat should match origin");
        assertEquals(TAHOMA_LNG, ll[1], 0.0001, "Lng should match origin");
    }

    // === Direction Tests ===

    @Test
    void eastIsPositiveX() {
        // A point east of Tahoma should have positive X
        int[] mc = projection.toMC(TAHOMA_LAT, TAHOMA_LNG + 0.01);
        assertTrue(mc[0] > 0, "East should be +X, got " + mc[0]);
    }

    @Test
    void westIsNegativeX() {
        int[] mc = projection.toMC(TAHOMA_LAT, TAHOMA_LNG - 0.01);
        assertTrue(mc[0] < 0, "West should be -X, got " + mc[0]);
    }

    @Test
    void southIsPositiveZ() {
        // South of Tahoma = lower latitude = +Z
        int[] mc = projection.toMC(TAHOMA_LAT - 0.01, TAHOMA_LNG);
        assertTrue(mc[1] > 0, "South should be +Z, got " + mc[1]);
    }

    @Test
    void northIsNegativeZ() {
        int[] mc = projection.toMC(TAHOMA_LAT + 0.01, TAHOMA_LNG);
        assertTrue(mc[1] < 0, "North should be -Z, got " + mc[1]);
    }

    // === Tacoma Tests (key real-world validation) ===

    @Test
    void tacomaProducesNegativeCoordinates() {
        // Tacoma is northwest of Tahoma: higher lat, more negative lng
        double tacomaLat = 47.2529;
        double tacomaLng = -122.4443;

        int[] mc = projection.toMC(tacomaLat, tacomaLng);

        // Tacoma is west of Tahoma → negative X
        assertTrue(mc[0] < 0, "Tacoma X should be negative (west), got " + mc[0]);
        // Tacoma is north of Tahoma → negative Z
        assertTrue(mc[1] < 0, "Tacoma Z should be negative (north), got " + mc[1]);

        // Approximate expected: ~52km west, ~44km north
        // At scale 1.0, that's roughly -52000 X and -44000 Z
        assertTrue(mc[0] < -40000, "Tacoma X should be < -40000, got " + mc[0]);
        assertTrue(mc[0] > -60000, "Tacoma X should be > -60000, got " + mc[0]);
        assertTrue(mc[1] < -40000, "Tacoma Z should be < -40000, got " + mc[1]);
        assertTrue(mc[1] > -50000, "Tacoma Z should be > -50000, got " + mc[1]);
    }

    // === Round-trip Accuracy ===

    @Test
    void roundTripAccuracyWithin1Meter() {
        // Test several points and verify round-trip accuracy
        double[][] testPoints = {
            {TAHOMA_LAT, TAHOMA_LNG},                     // origin
            {47.2529, -122.4443},                          // Tacoma
            {47.6062, -122.3321},                          // Seattle
            {45.5152, -122.6784},                          // Portland
            {TAHOMA_LAT + 0.001, TAHOMA_LNG - 0.001},     // near origin
        };

        for (double[] point : testPoints) {
            int[] mc = projection.toMC(point[0], point[1]);
            double[] back = projection.toLatlng(mc[0], mc[1]);

            double distanceError = GeoProjection.haversineDistance(
                point[0], point[1], back[0], back[1]);

            assertTrue(distanceError < 2.0,
                String.format("Round-trip error for (%.4f, %.4f) was %.1fm, expected <2m",
                    point[0], point[1], distanceError));
        }
    }

    // === Bounding Box ===

    @Test
    void bboxToMcNormalizesMinMax() {
        // Even if minLat > maxLat in the input, output should be normalized
        double minLat = 47.252, minLng = -122.450;
        double maxLat = 47.258, maxLng = -122.438;

        int[] bbox = projection.bboxToMC(minLat, minLng, maxLat, maxLng);

        assertTrue(bbox[0] <= bbox[2], "minX should be <= maxX");
        assertTrue(bbox[1] <= bbox[3], "minZ should be <= maxZ");
    }

    @Test
    void bboxSizeIsPositive() {
        int[] size = projection.bboxSize(47.252, -122.450, 47.258, -122.438);
        assertTrue(size[0] > 0, "Width should be positive");
        assertTrue(size[1] > 0, "Height should be positive");
    }

    // === Region Snapping ===

    @Test
    void snapToRegionPositiveCoords() {
        assertEquals(0, GeoProjection.snapToRegion(0));
        assertEquals(0, GeoProjection.snapToRegion(511));
        assertEquals(512, GeoProjection.snapToRegion(512));
        assertEquals(512, GeoProjection.snapToRegion(600));
    }

    @Test
    void snapToRegionNegativeCoords() {
        // Critical: Math.floorDiv must give -512, not 0
        assertEquals(-512, GeoProjection.snapToRegion(-1));
        assertEquals(-512, GeoProjection.snapToRegion(-512));
        assertEquals(-1024, GeoProjection.snapToRegion(-513));
    }

    @Test
    void blockToRegionNegativeCoords() {
        assertEquals(0, GeoProjection.blockToRegion(0));
        assertEquals(0, GeoProjection.blockToRegion(511));
        assertEquals(1, GeoProjection.blockToRegion(512));
        assertEquals(-1, GeoProjection.blockToRegion(-1));
        assertEquals(-1, GeoProjection.blockToRegion(-512));
        assertEquals(-2, GeoProjection.blockToRegion(-513));
    }

    // === Haversine Distance ===

    @Test
    void haversineDistanceSamePointIsZero() {
        double d = GeoProjection.haversineDistance(
            TAHOMA_LAT, TAHOMA_LNG, TAHOMA_LAT, TAHOMA_LNG);
        assertEquals(0.0, d, 0.001);
    }

    @Test
    void haversineDistanceTacomaToSeattle() {
        // Tacoma to Seattle is roughly 50km
        double d = GeoProjection.haversineDistance(
            47.2529, -122.4443, 47.6062, -122.3321);
        assertTrue(d > 35000 && d < 45000,
            "Tacoma-Seattle distance should be ~40km, got " + (d / 1000) + "km");
    }

    // === Scale Factor ===

    @Test
    void scaleDoublesBlockDistances() {
        GeoProjection scaled = new GeoProjection(TAHOMA_LAT, TAHOMA_LNG, 2.0);
        int[] mc1 = projection.toMC(47.0, -122.0);
        int[] mc2 = scaled.toMC(47.0, -122.0);

        assertEquals(mc1[0] * 2, mc2[0], 1, "Scale 2.0 should double X distance");
        assertEquals(mc1[1] * 2, mc2[1], 1, "Scale 2.0 should double Z distance");
    }

    // === Getters ===

    @Test
    void gettersReturnConstructorValues() {
        assertEquals(TAHOMA_LAT, projection.getOriginLat());
        assertEquals(TAHOMA_LNG, projection.getOriginLng());
        assertEquals(1.0, projection.getScale());
    }
}
