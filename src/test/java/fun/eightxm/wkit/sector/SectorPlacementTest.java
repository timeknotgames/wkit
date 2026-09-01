package fun.eightxm.wkit.sector;

import fun.eightxm.wkit.region.RegionMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SectorPlacement — region-aligned bbox snapping.
 * Uses real GeoProjection (pure math, no mocking needed).
 */
class SectorPlacementTest {

    private static final double TAHOMA_LAT = 46.8523;
    private static final double TAHOMA_LNG = -121.7603;

    private GeoProjection projection;
    private SectorPlacement placement;

    @BeforeEach
    void setUp() {
        projection = new GeoProjection(TAHOMA_LAT, TAHOMA_LNG, 1.0);
        placement = new SectorPlacement(projection);
    }

    // === alignToRegions ===

    @Test
    void alignedBoundsAreMultiplesOf512() {
        SectorPlacement.RegionAlignedBBox bbox = placement.alignToRegions(
            47.252, -122.450, 47.258, -122.438);

        assertEquals(0, bbox.mcMinX % RegionMath.REGION_SIZE,
            "minX should be a multiple of 512");
        assertEquals(0, bbox.mcMinZ % RegionMath.REGION_SIZE,
            "minZ should be a multiple of 512");
        assertEquals(0, (bbox.mcMaxX + 1) % RegionMath.REGION_SIZE,
            "maxX+1 should be a multiple of 512");
        assertEquals(0, (bbox.mcMaxZ + 1) % RegionMath.REGION_SIZE,
            "maxZ+1 should be a multiple of 512");
    }

    @Test
    void alignedBoundsContainOriginalBbox() {
        // Original MC projection
        int[] mcMin = projection.toMC(47.252, -122.450);
        int[] mcMax = projection.toMC(47.258, -122.438);
        int rawMinX = Math.min(mcMin[0], mcMax[0]);
        int rawMinZ = Math.min(mcMin[1], mcMax[1]);
        int rawMaxX = Math.max(mcMin[0], mcMax[0]);
        int rawMaxZ = Math.max(mcMin[1], mcMax[1]);

        SectorPlacement.RegionAlignedBBox bbox = placement.alignToRegions(
            47.252, -122.450, 47.258, -122.438);

        assertTrue(bbox.mcMinX <= rawMinX, "Aligned minX should be <= raw minX");
        assertTrue(bbox.mcMinZ <= rawMinZ, "Aligned minZ should be <= raw minZ");
        assertTrue(bbox.mcMaxX >= rawMaxX, "Aligned maxX should be >= raw maxX");
        assertTrue(bbox.mcMaxZ >= rawMaxZ, "Aligned maxZ should be >= raw maxZ");
    }

    @Test
    void alignedBoundsWithNegativeCoords() {
        // Tacoma is northwest of Tahoma — should produce negative MC coords
        SectorPlacement.RegionAlignedBBox bbox = placement.alignToRegions(
            47.252, -122.450, 47.258, -122.438);

        assertTrue(bbox.mcMinX < 0, "Tacoma X should be negative");
        assertTrue(bbox.mcMinZ < 0, "Tacoma Z should be negative");
        assertEquals(0, bbox.mcMinX % RegionMath.REGION_SIZE,
            "Negative minX should still align to 512");
    }

    @Test
    void widthAndHeightArePositive() {
        SectorPlacement.RegionAlignedBBox bbox = placement.alignToRegions(
            47.252, -122.450, 47.258, -122.438);

        assertTrue(bbox.widthBlocks() > 0, "Width should be positive");
        assertTrue(bbox.heightBlocks() > 0, "Height should be positive");
        assertTrue(bbox.widthRegions() > 0, "Region width should be positive");
        assertTrue(bbox.heightRegions() > 0, "Region height should be positive");
    }

    @Test
    void widthBlocksMatchesRegionCount() {
        SectorPlacement.RegionAlignedBBox bbox = placement.alignToRegions(
            47.252, -122.450, 47.258, -122.438);

        assertEquals(bbox.widthRegions() * RegionMath.REGION_SIZE, bbox.widthBlocks(),
            "Width in blocks should equal regions * 512");
        assertEquals(bbox.heightRegions() * RegionMath.REGION_SIZE, bbox.heightBlocks(),
            "Height in blocks should equal regions * 512");
    }

    // === regionAt ===

    @Test
    void regionAtOriginIsRegionZero() {
        SectorPlacement.RegionAlignedBBox bbox = placement.regionAt(0, 0);

        assertEquals(0, bbox.regionMinX);
        assertEquals(0, bbox.regionMinZ);
        assertEquals(0, bbox.mcMinX);
        assertEquals(0, bbox.mcMinZ);
        assertEquals(511, bbox.mcMaxX);
        assertEquals(511, bbox.mcMaxZ);
    }

    @Test
    void regionAtNegativeCoords() {
        SectorPlacement.RegionAlignedBBox bbox = placement.regionAt(-1, -1);

        assertEquals(-1, bbox.regionMinX);
        assertEquals(-1, bbox.regionMinZ);
        assertEquals(-512, bbox.mcMinX);
        assertEquals(-512, bbox.mcMinZ);
        assertEquals(-1, bbox.mcMaxX);
        assertEquals(-1, bbox.mcMaxZ);
    }

    @Test
    void regionAtSingleRegionSize() {
        SectorPlacement.RegionAlignedBBox bbox = placement.regionAt(100, 200);

        assertEquals(1, bbox.widthRegions());
        assertEquals(1, bbox.heightRegions());
        assertEquals(512, bbox.widthBlocks());
        assertEquals(512, bbox.heightBlocks());
    }

    // === regionCluster ===

    @Test
    void regionClusterRadiusZeroIsSingleRegion() {
        SectorPlacement.RegionAlignedBBox bbox = placement.regionCluster(0, 0, 0);

        assertEquals(1, bbox.widthRegions());
        assertEquals(1, bbox.heightRegions());
    }

    @Test
    void regionClusterRadiusOneIs3x3() {
        SectorPlacement.RegionAlignedBBox bbox = placement.regionCluster(0, 0, 1);

        assertEquals(3, bbox.widthRegions());
        assertEquals(3, bbox.heightRegions());
        assertEquals(3 * 512, bbox.widthBlocks());
    }

    @Test
    void regionClusterCenteredOnNegativeCoords() {
        SectorPlacement.RegionAlignedBBox bbox = placement.regionCluster(-256, -256, 1);

        // Center is in region (-1, -1), radius 1 → regions (-2,-2) to (0,0)
        assertEquals(-2, bbox.regionMinX);
        assertEquals(-2, bbox.regionMinZ);
        assertEquals(0, bbox.regionMaxX);
        assertEquals(0, bbox.regionMaxZ);
    }

    // === Adjacent sectors have no gaps ===

    @Test
    void adjacentRegionsHaveNoGaps() {
        SectorPlacement.RegionAlignedBBox region0 = placement.regionAt(0, 0);
        SectorPlacement.RegionAlignedBBox region1 = placement.regionAt(512, 0);

        assertEquals(region0.mcMaxX + 1, region1.mcMinX,
            "Adjacent regions should have no gap (maxX+1 = next minX)");
    }

    @Test
    void adjacentNegativeRegionsHaveNoGaps() {
        SectorPlacement.RegionAlignedBBox regionNeg1 = placement.regionAt(-1, 0);
        SectorPlacement.RegionAlignedBBox region0 = placement.regionAt(0, 0);

        assertEquals(regionNeg1.mcMaxX + 1, region0.mcMinX,
            "Negative-to-positive region boundary should be seamless");
    }

    // === toBboxString ===

    @Test
    void toBboxStringFormat() {
        SectorPlacement.RegionAlignedBBox bbox = placement.regionAt(0, 0);
        String str = bbox.toBboxString();

        // Should be "lat,lng,lat,lng" format
        String[] parts = str.split(",");
        assertEquals(4, parts.length, "bbox string should have 4 comma-separated values");
    }
}
