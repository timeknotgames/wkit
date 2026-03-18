package fun.eightxm.wkit.sector;

import fun.eightxm.wkit.region.RegionMath;

/**
 * Handles region-aligned sector placement.
 * Ensures all sector boundaries align with 512-block region boundaries
 * so adjacent sectors tile perfectly with zero gaps or overlap.
 */
public class SectorPlacement {

    private final GeoProjection projection;

    public SectorPlacement(GeoProjection projection) {
        this.projection = projection;
    }

    /**
     * Snap a geographic bbox to region-aligned boundaries.
     *
     * 1. Project geo bbox to MC coordinates
     * 2. Snap min DOWN and max UP to nearest region boundaries (multiples of 512)
     * 3. Project back to geographic bbox
     *
     * This ensures Arnis generates content that fills exact whole regions.
     *
     * @return RegionAlignedBBox with both geographic and MC bounds
     */
    public RegionAlignedBBox alignToRegions(double minLat, double minLng, double maxLat, double maxLng) {
        // Project to MC
        int[] mcMin = projection.toMC(minLat, minLng);
        int[] mcMax = projection.toMC(maxLat, maxLng);

        // Normalize (ensure min < max)
        int rawMinX = Math.min(mcMin[0], mcMax[0]);
        int rawMinZ = Math.min(mcMin[1], mcMax[1]);
        int rawMaxX = Math.max(mcMin[0], mcMax[0]);
        int rawMaxZ = Math.max(mcMin[1], mcMax[1]);

        // Snap min DOWN to region boundary, max UP to next region boundary
        int alignedMinX = Math.floorDiv(rawMinX, RegionMath.REGION_SIZE) * RegionMath.REGION_SIZE;
        int alignedMinZ = Math.floorDiv(rawMinZ, RegionMath.REGION_SIZE) * RegionMath.REGION_SIZE;
        int alignedMaxX = (Math.floorDiv(rawMaxX, RegionMath.REGION_SIZE) + 1) * RegionMath.REGION_SIZE - 1;
        int alignedMaxZ = (Math.floorDiv(rawMaxZ, RegionMath.REGION_SIZE) + 1) * RegionMath.REGION_SIZE - 1;

        // Project back to geographic
        double[] geoMin = projection.toLatlng(alignedMinX, alignedMaxZ); // maxZ = southern edge (lower lat)
        double[] geoMax = projection.toLatlng(alignedMaxX, alignedMinZ); // minZ = northern edge (higher lat)

        return new RegionAlignedBBox(
            geoMin[0], geoMin[1], geoMax[0], geoMax[1],    // geo bounds (snapped)
            alignedMinX, alignedMinZ, alignedMaxX, alignedMaxZ,  // MC bounds
            RegionMath.blockToRegion(alignedMinX),           // region coords
            RegionMath.blockToRegion(alignedMinZ),
            RegionMath.blockToRegion(alignedMaxX),
            RegionMath.blockToRegion(alignedMaxZ)
        );
    }

    /**
     * Compute the region-aligned sector for a given MC position.
     * Returns the bbox of the region(s) that would cover this position
     * as a single-region sector (512x512 blocks).
     */
    public RegionAlignedBBox regionAt(int mcX, int mcZ) {
        int regionX = RegionMath.blockToRegion(mcX);
        int regionZ = RegionMath.blockToRegion(mcZ);

        int minX = regionX * RegionMath.REGION_SIZE;
        int minZ = regionZ * RegionMath.REGION_SIZE;
        int maxX = minX + RegionMath.REGION_SIZE - 1;
        int maxZ = minZ + RegionMath.REGION_SIZE - 1;

        double[] geoMin = projection.toLatlng(minX, maxZ);
        double[] geoMax = projection.toLatlng(maxX, minZ);

        return new RegionAlignedBBox(
            geoMin[0], geoMin[1], geoMax[0], geoMax[1],
            minX, minZ, maxX, maxZ,
            regionX, regionZ, regionX, regionZ
        );
    }

    /**
     * Compute the N-region-wide sector centered on a position.
     * Good for procedural generation — generates a cluster of regions at once.
     */
    public RegionAlignedBBox regionCluster(int mcX, int mcZ, int radiusRegions) {
        int centerRegionX = RegionMath.blockToRegion(mcX);
        int centerRegionZ = RegionMath.blockToRegion(mcZ);

        int minRegionX = centerRegionX - radiusRegions;
        int minRegionZ = centerRegionZ - radiusRegions;
        int maxRegionX = centerRegionX + radiusRegions;
        int maxRegionZ = centerRegionZ + radiusRegions;

        int minX = minRegionX * RegionMath.REGION_SIZE;
        int minZ = minRegionZ * RegionMath.REGION_SIZE;
        int maxX = (maxRegionX + 1) * RegionMath.REGION_SIZE - 1;
        int maxZ = (maxRegionZ + 1) * RegionMath.REGION_SIZE - 1;

        double[] geoMin = projection.toLatlng(minX, maxZ);
        double[] geoMax = projection.toLatlng(maxX, minZ);

        return new RegionAlignedBBox(
            geoMin[0], geoMin[1], geoMax[0], geoMax[1],
            minX, minZ, maxX, maxZ,
            minRegionX, minRegionZ, maxRegionX, maxRegionZ
        );
    }

    /**
     * Holds a region-aligned bounding box with both geographic and MC coordinates.
     */
    public static class RegionAlignedBBox {
        public final double geoMinLat, geoMinLng, geoMaxLat, geoMaxLng;
        public final int mcMinX, mcMinZ, mcMaxX, mcMaxZ;
        public final int regionMinX, regionMinZ, regionMaxX, regionMaxZ;

        public RegionAlignedBBox(double geoMinLat, double geoMinLng, double geoMaxLat, double geoMaxLng,
                                  int mcMinX, int mcMinZ, int mcMaxX, int mcMaxZ,
                                  int regionMinX, int regionMinZ, int regionMaxX, int regionMaxZ) {
            this.geoMinLat = geoMinLat;
            this.geoMinLng = geoMinLng;
            this.geoMaxLat = geoMaxLat;
            this.geoMaxLng = geoMaxLng;
            this.mcMinX = mcMinX;
            this.mcMinZ = mcMinZ;
            this.mcMaxX = mcMaxX;
            this.mcMaxZ = mcMaxZ;
            this.regionMinX = regionMinX;
            this.regionMinZ = regionMinZ;
            this.regionMaxX = regionMaxX;
            this.regionMaxZ = regionMaxZ;
        }

        public String toBboxString() {
            return geoMinLat + "," + geoMinLng + "," + geoMaxLat + "," + geoMaxLng;
        }

        public int widthBlocks() { return mcMaxX - mcMinX + 1; }
        public int heightBlocks() { return mcMaxZ - mcMinZ + 1; }
        public int widthRegions() { return regionMaxX - regionMinX + 1; }
        public int heightRegions() { return regionMaxZ - regionMinZ + 1; }

        @Override
        public String toString() {
            return widthRegions() + "x" + heightRegions() + " regions ("
                + widthBlocks() + "x" + heightBlocks() + " blocks) at r."
                + regionMinX + "." + regionMinZ + " → r." + regionMaxX + "." + regionMaxZ;
        }
    }
}
