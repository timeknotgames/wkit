package fun.eightxm.wkit.generator;

/**
 * Piecewise elevation mapping that preserves local terrain detail at ALL elevations.
 *
 * The problem: Mount Tahoma (4392m) and downtown Tacoma (30m) in the same world.
 * Linear compression (366 blocks / 4392m = 0.083 blocks/meter) makes Tacoma's
 * 30m hills invisible (2.5 blocks). Players can't walk meaningful terrain.
 *
 * The solution: logarithmic-inspired piecewise mapping that gives MORE vertical
 * resolution to lower elevations (where cities are) and LESS to high mountains.
 * Players walking from Tacoma to Tahoma experience a continuous, walkable gradient
 * where terrain is always visually meaningful.
 *
 * Mapping (meters → Minecraft Y):
 *   -50m to 0m   → Y=-62 to Y=-12  (1.0 block/meter — underwater detail)
 *     0m to 200m  → Y=-12 to Y=188  (1.0 block/meter — full city detail!)
 *   200m to 1000m → Y=188 to Y=228  (0.05 block/meter — foothills)
 *  1000m to 3000m → Y=228 to Y=278  (0.025 block/meter — mountain flanks)
 *  3000m to 5000m → Y=278 to Y=304  (0.013 block/meter — summit zone)
 *
 * This means:
 *   - A 30m hill in Tacoma = 30 blocks tall (VERY visible, walkable)
 *   - Downtown Tacoma at 60m = Y=48 (comfortable ground level)
 *   - Foothills at 500m = Y=213
 *   - Mount Tahoma summit at 4392m = Y=296
 *   - Sea level = Y=-12
 */
public class ElevationMapper {

    // Piecewise breakpoints: {elevation_meters, minecraft_Y}
    private static final double[][] BREAKPOINTS = {
        {-50.0,   -62.0},
        {0.0,     -12.0},
        {200.0,   188.0},
        {1000.0,  228.0},
        {3000.0,  278.0},
        {5000.0,  304.0},
    };

    /**
     * Convert real-world elevation (meters) to Minecraft Y coordinate.
     * Uses piecewise linear interpolation for walkable terrain at all elevations.
     */
    public static int elevationToY(double elevationMeters) {
        // Clamp to range
        if (elevationMeters <= BREAKPOINTS[0][0]) {
            return (int) BREAKPOINTS[0][1];
        }
        if (elevationMeters >= BREAKPOINTS[BREAKPOINTS.length - 1][0]) {
            return (int) BREAKPOINTS[BREAKPOINTS.length - 1][1];
        }

        // Find the segment
        for (int i = 0; i < BREAKPOINTS.length - 1; i++) {
            double e0 = BREAKPOINTS[i][0];
            double y0 = BREAKPOINTS[i][1];
            double e1 = BREAKPOINTS[i + 1][0];
            double y1 = BREAKPOINTS[i + 1][1];

            if (elevationMeters >= e0 && elevationMeters <= e1) {
                double t = (elevationMeters - e0) / (e1 - e0);
                return (int) Math.round(y0 + t * (y1 - y0));
            }
        }

        return -12; // sea level fallback
    }

    /**
     * Convert Minecraft Y back to approximate elevation in meters.
     */
    public static double yToElevation(int mcY) {
        for (int i = 0; i < BREAKPOINTS.length - 1; i++) {
            double e0 = BREAKPOINTS[i][0];
            double y0 = BREAKPOINTS[i][1];
            double e1 = BREAKPOINTS[i + 1][0];
            double y1 = BREAKPOINTS[i + 1][1];

            if (mcY >= y0 && mcY <= y1) {
                double t = (mcY - y0) / (y1 - y0);
                return e0 + t * (e1 - e0);
            }
        }
        return 0;
    }

    /**
     * Get the sea level Y coordinate.
     */
    public static int getSeaLevelY() {
        return elevationToY(0);
    }
}
