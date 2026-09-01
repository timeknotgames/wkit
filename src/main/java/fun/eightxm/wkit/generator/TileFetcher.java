package fun.eightxm.wkit.generator;

/**
 * Abstracts elevation tile fetching for testability.
 * Default implementation fetches from AWS Terrarium via HTTP.
 * Tests can provide mock implementations with pre-built elevation grids.
 */
@FunctionalInterface
public interface TileFetcher {

    /**
     * Fetch a decoded elevation grid for the given tile coordinates.
     * The grid is TILE_SIZE x TILE_SIZE (256x256) floats representing
     * elevation in meters at each pixel.
     *
     * @return decoded elevation grid, or null if the tile cannot be fetched
     */
    float[][] fetchTile(int tileX, int tileY);
}
