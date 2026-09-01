package fun.eightxm.wkit.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ElevationMapper — piecewise elevation mapping.
 *
 * Mapping:
 *   -50m to 0m   → Y=-62 to Y=-12  (1.0 block/meter)
 *     0m to 200m  → Y=-12 to Y=188  (1.0 block/meter)
 *   200m to 1000m → Y=188 to Y=228  (0.05 block/meter)
 *  1000m to 3000m → Y=228 to Y=278  (0.025 block/meter)
 *  3000m to 5000m → Y=278 to Y=304  (0.013 block/meter)
 */
class ElevationMapperTest {

    // === Breakpoint Boundary Tests ===

    @Test
    void seaLevelMapsToYMinus12() {
        assertEquals(-12, ElevationMapper.elevationToY(0.0));
    }

    @Test
    void minus50mMapsToYMinus62() {
        assertEquals(-62, ElevationMapper.elevationToY(-50.0));
    }

    @Test
    void elevation200mMapsToY188() {
        assertEquals(188, ElevationMapper.elevationToY(200.0));
    }

    @Test
    void elevation1000mMapsToY228() {
        assertEquals(228, ElevationMapper.elevationToY(1000.0));
    }

    @Test
    void elevation3000mMapsToY278() {
        assertEquals(278, ElevationMapper.elevationToY(3000.0));
    }

    @Test
    void elevation5000mMapsToY304() {
        assertEquals(304, ElevationMapper.elevationToY(5000.0));
    }

    // === Key Landmark Tests ===

    @Test
    void tacomaSixtyMetersMapsToY48() {
        // Downtown Tacoma at ~60m should map to Y=48
        // 60m is in the 0-200m band: Y = -12 + (60/200) * 200 = -12 + 60 = 48
        assertEquals(48, ElevationMapper.elevationToY(60.0));
    }

    @Test
    void tahomaSummitMapsToApproxY296() {
        // Mount Tahoma at 4392m
        // In 3000-5000m band: Y = 278 + ((4392-3000)/(5000-3000)) * 26
        // = 278 + (1392/2000) * 26 = 278 + 18.1 ≈ 296
        int y = ElevationMapper.elevationToY(4392.0);
        assertTrue(y >= 294 && y <= 298,
            "Tahoma summit should be ~Y=296, got " + y);
    }

    @Test
    void getSeaLevelYReturnsMinus12() {
        assertEquals(-12, ElevationMapper.getSeaLevelY());
    }

    // === Clamping Tests ===

    @Test
    void belowMinClamps() {
        // Below -50m should clamp to -62
        assertEquals(-62, ElevationMapper.elevationToY(-100.0));
        assertEquals(-62, ElevationMapper.elevationToY(-500.0));
    }

    @Test
    void aboveMaxClamps() {
        // Above 5000m should clamp to 304
        assertEquals(304, ElevationMapper.elevationToY(6000.0));
        assertEquals(304, ElevationMapper.elevationToY(10000.0));
    }

    // === Interpolation Tests ===

    @Test
    void midpointOfFirstBand() {
        // -25m should be midpoint of -62 to -12 → Y=-37
        assertEquals(-37, ElevationMapper.elevationToY(-25.0));
    }

    @Test
    void midpointOfCityBand() {
        // 100m should be midpoint of -12 to 188 → Y=88
        assertEquals(88, ElevationMapper.elevationToY(100.0));
    }

    // === Round-trip Tests ===

    @Test
    void roundTripAtBreakpoints() {
        // At breakpoints, round-trip should be exact
        double[] breakpointElevations = {-50.0, 0.0, 200.0, 1000.0, 3000.0, 5000.0};
        for (double elev : breakpointElevations) {
            int y = ElevationMapper.elevationToY(elev);
            double back = ElevationMapper.yToElevation(y);
            assertEquals(elev, back, 1.0,
                "Round-trip at " + elev + "m: Y=" + y + " → " + back + "m");
        }
    }

    // === Monotonicity Test ===

    @Test
    void higherElevationGivesHigherOrEqualY() {
        // Elevation should always increase Y (monotonically non-decreasing)
        int prevY = Integer.MIN_VALUE;
        for (double elev = -50; elev <= 5000; elev += 10) {
            int y = ElevationMapper.elevationToY(elev);
            assertTrue(y >= prevY,
                "Y should be monotonically non-decreasing: at " + elev + "m, Y=" + y + " < prevY=" + prevY);
            prevY = y;
        }
    }
}
