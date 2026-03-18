package fun.eightxm.wkit.sector;

/**
 * Geographic projection centered on Mount Tahoma (Mother of Waters).
 * Converts between lat/lng and Minecraft XZ coordinates.
 * Mirrors the Haversine-based math from Arnis's CoordTransformer.
 */
public class GeoProjection {

    private static final double EARTH_RADIUS = 6_371_000.0; // meters

    private final double originLat;
    private final double originLng;
    private final double scale;

    // Meters per degree at the origin latitude
    private final double metersPerDegreeLat;
    private final double metersPerDegreeLng;

    public GeoProjection(double originLat, double originLng, double scale) {
        this.originLat = originLat;
        this.originLng = originLng;
        this.scale = scale;
        this.metersPerDegreeLat = latDistance();
        this.metersPerDegreeLng = lonDistance(originLat);
    }

    /**
     * Convert lat/lng to Minecraft XZ relative to the origin.
     * East = +X, South = +Z (north = -Z, west = -X).
     */
    public int[] toMC(double lat, double lng) {
        double dLng = lng - originLng;
        double dLat = originLat - lat; // inverted: north = -Z, so origin-lat = +Z for points north

        int mcX = (int) Math.round(dLng * metersPerDegreeLng * scale);
        int mcZ = (int) Math.round(dLat * metersPerDegreeLat * scale);

        return new int[]{mcX, mcZ};
    }

    /**
     * Convert Minecraft XZ back to lat/lng.
     */
    public double[] toLatlng(int mcX, int mcZ) {
        double lng = originLng + (mcX / (metersPerDegreeLng * scale));
        double lat = originLat - (mcZ / (metersPerDegreeLat * scale));
        return new double[]{lat, lng};
    }

    /**
     * Compute the MC bounding box for a geographic bbox.
     * Returns [minX, minZ, maxX, maxZ].
     */
    public int[] bboxToMC(double minLat, double minLng, double maxLat, double maxLng) {
        int[] sw = toMC(minLat, minLng);
        int[] ne = toMC(maxLat, maxLng);
        return new int[]{
            Math.min(sw[0], ne[0]),
            Math.min(sw[1], ne[1]),
            Math.max(sw[0], ne[0]),
            Math.max(sw[1], ne[1])
        };
    }

    /**
     * Get the MC dimensions (width, height in blocks) for a geographic bbox.
     */
    public int[] bboxSize(double minLat, double minLng, double maxLat, double maxLng) {
        int[] mc = bboxToMC(minLat, minLng, maxLat, maxLng);
        return new int[]{mc[2] - mc[0], mc[3] - mc[1]};
    }

    /**
     * Snap a block coordinate down to the nearest region boundary (multiple of 512).
     */
    public static int snapToRegion(int blockCoord) {
        return Math.floorDiv(blockCoord, 512) * 512;
    }

    /**
     * Convert block coordinate to region coordinate.
     */
    public static int blockToRegion(int blockCoord) {
        return Math.floorDiv(blockCoord, 512);
    }

    /**
     * Meters per degree of latitude (approximately constant).
     */
    private static double latDistance() {
        // Haversine for 1 degree latitude
        double dLat = Math.toRadians(1.0);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * Meters per degree of longitude at a given latitude.
     */
    private static double lonDistance(double latitude) {
        double dLng = Math.toRadians(1.0);
        double lat = Math.toRadians(latitude);
        double a = Math.cos(lat) * Math.cos(lat) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    /**
     * Compute distance in meters between two lat/lng points.
     */
    public static double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public double getOriginLat() { return originLat; }
    public double getOriginLng() { return originLng; }
    public double getScale() { return scale; }
}
