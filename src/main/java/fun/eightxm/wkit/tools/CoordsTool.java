package fun.eightxm.wkit.tools;

import fun.eightxm.wkit.sector.GeoProjection;

/**
 * Converts MC coordinates to lat/lng and provides distance calculations.
 */
public class CoordsTool {

    private final GeoProjection projection;

    public CoordsTool(GeoProjection projection) {
        this.projection = projection;
    }

    /**
     * Get lat/lng for an MC position.
     */
    public double[] getLatLng(int mcX, int mcZ) {
        return projection.toLatlng(mcX, mcZ);
    }

    /**
     * Get distance in meters between two MC positions.
     */
    public double distanceMeters(int x1, int z1, int x2, int z2) {
        double[] ll1 = projection.toLatlng(x1, z1);
        double[] ll2 = projection.toLatlng(x2, z2);
        return GeoProjection.haversineDistance(ll1[0], ll1[1], ll2[0], ll2[1]);
    }

    /**
     * Get MC block distance between two positions (Euclidean in XZ plane).
     */
    public double distanceBlocks(int x1, int z1, int x2, int z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
