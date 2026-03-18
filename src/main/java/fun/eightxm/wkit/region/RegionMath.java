package fun.eightxm.wkit.region;

/**
 * Minecraft coordinate math for regions, chunks, and blocks.
 * A region is 512x512 blocks (32x32 chunks).
 * A chunk is 16x16 blocks.
 */
public class RegionMath {

    public static final int REGION_SIZE = 512;  // blocks per region side
    public static final int CHUNK_SIZE = 16;    // blocks per chunk side
    public static final int CHUNKS_PER_REGION = 32; // chunks per region side

    /** Block coordinate to region coordinate. */
    public static int blockToRegion(int block) {
        return Math.floorDiv(block, REGION_SIZE);
    }

    /** Block coordinate to chunk coordinate (absolute). */
    public static int blockToChunk(int block) {
        return Math.floorDiv(block, CHUNK_SIZE);
    }

    /** Chunk coordinate to region coordinate. */
    public static int chunkToRegion(int chunk) {
        return Math.floorDiv(chunk, CHUNKS_PER_REGION);
    }

    /** Chunk coordinate within its region (0-31). */
    public static int chunkLocalInRegion(int chunk) {
        return Math.floorMod(chunk, CHUNKS_PER_REGION);
    }

    /** Region coordinate to the starting block coordinate. */
    public static int regionToBlock(int region) {
        return region * REGION_SIZE;
    }

    /** Snap a block coordinate down to region boundary. */
    public static int snapToRegionBoundary(int block) {
        return blockToRegion(block) * REGION_SIZE;
    }

    /** Build the region file name for given region coordinates. */
    public static String regionFileName(int regionX, int regionZ) {
        return "r." + regionX + "." + regionZ + ".mca";
    }
}
