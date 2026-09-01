package fun.eightxm.wkit.region;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegionMath — coordinate math for regions, chunks, and blocks.
 * Critical: negative coordinate handling must use Math.floorDiv, not integer division.
 */
class RegionMathTest {

    // === Block to Region ===

    @Test
    void blockToRegionAtOrigin() {
        assertEquals(0, RegionMath.blockToRegion(0));
    }

    @Test
    void blockToRegionWithinFirstRegion() {
        assertEquals(0, RegionMath.blockToRegion(1));
        assertEquals(0, RegionMath.blockToRegion(511));
    }

    @Test
    void blockToRegionAtBoundary() {
        assertEquals(1, RegionMath.blockToRegion(512));
        assertEquals(1, RegionMath.blockToRegion(1023));
        assertEquals(2, RegionMath.blockToRegion(1024));
    }

    @Test
    void blockToRegionNegativeCoords() {
        // This is the critical test: -1 should be in region -1, not region 0
        assertEquals(-1, RegionMath.blockToRegion(-1));
        assertEquals(-1, RegionMath.blockToRegion(-512));
        assertEquals(-2, RegionMath.blockToRegion(-513));
        assertEquals(-2, RegionMath.blockToRegion(-1024));
        assertEquals(-3, RegionMath.blockToRegion(-1025));
    }

    // === Block to Chunk ===

    @Test
    void blockToChunkPositive() {
        assertEquals(0, RegionMath.blockToChunk(0));
        assertEquals(0, RegionMath.blockToChunk(15));
        assertEquals(1, RegionMath.blockToChunk(16));
    }

    @Test
    void blockToChunkNegative() {
        assertEquals(-1, RegionMath.blockToChunk(-1));
        assertEquals(-1, RegionMath.blockToChunk(-16));
        assertEquals(-2, RegionMath.blockToChunk(-17));
    }

    // === Chunk to Region ===

    @Test
    void chunkToRegionPositive() {
        assertEquals(0, RegionMath.chunkToRegion(0));
        assertEquals(0, RegionMath.chunkToRegion(31));
        assertEquals(1, RegionMath.chunkToRegion(32));
    }

    @Test
    void chunkToRegionNegative() {
        assertEquals(-1, RegionMath.chunkToRegion(-1));
        assertEquals(-1, RegionMath.chunkToRegion(-32));
        assertEquals(-2, RegionMath.chunkToRegion(-33));
    }

    // === Chunk Local in Region ===

    @Test
    void chunkLocalPositive() {
        assertEquals(0, RegionMath.chunkLocalInRegion(0));
        assertEquals(1, RegionMath.chunkLocalInRegion(1));
        assertEquals(31, RegionMath.chunkLocalInRegion(31));
        assertEquals(0, RegionMath.chunkLocalInRegion(32));
    }

    @Test
    void chunkLocalNegative() {
        // Math.floorMod(-1, 32) = 31
        assertEquals(31, RegionMath.chunkLocalInRegion(-1));
        assertEquals(0, RegionMath.chunkLocalInRegion(-32));
        assertEquals(31, RegionMath.chunkLocalInRegion(-33));
    }

    // === Region to Block ===

    @Test
    void regionToBlock() {
        assertEquals(0, RegionMath.regionToBlock(0));
        assertEquals(512, RegionMath.regionToBlock(1));
        assertEquals(-512, RegionMath.regionToBlock(-1));
        assertEquals(-1024, RegionMath.regionToBlock(-2));
    }

    // === Snap to Region Boundary ===

    @Test
    void snapToRegionBoundaryPositive() {
        assertEquals(0, RegionMath.snapToRegionBoundary(0));
        assertEquals(0, RegionMath.snapToRegionBoundary(511));
        assertEquals(512, RegionMath.snapToRegionBoundary(512));
    }

    @Test
    void snapToRegionBoundaryNegative() {
        assertEquals(-512, RegionMath.snapToRegionBoundary(-1));
        assertEquals(-512, RegionMath.snapToRegionBoundary(-512));
        assertEquals(-1024, RegionMath.snapToRegionBoundary(-513));
    }

    // === Region File Name ===

    @Test
    void regionFileNamePositive() {
        assertEquals("r.3.5.mca", RegionMath.regionFileName(3, 5));
    }

    @Test
    void regionFileNameNegative() {
        assertEquals("r.-3.5.mca", RegionMath.regionFileName(-3, 5));
        assertEquals("r.0.-1.mca", RegionMath.regionFileName(0, -1));
    }

    @Test
    void regionFileNameOrigin() {
        assertEquals("r.0.0.mca", RegionMath.regionFileName(0, 0));
    }

    // === Constants ===

    @Test
    void constantsAreCorrect() {
        assertEquals(512, RegionMath.REGION_SIZE);
        assertEquals(16, RegionMath.CHUNK_SIZE);
        assertEquals(32, RegionMath.CHUNKS_PER_REGION);
        // Verify relationship: REGION_SIZE = CHUNK_SIZE * CHUNKS_PER_REGION
        assertEquals(RegionMath.REGION_SIZE,
            RegionMath.CHUNK_SIZE * RegionMath.CHUNKS_PER_REGION);
    }
}
