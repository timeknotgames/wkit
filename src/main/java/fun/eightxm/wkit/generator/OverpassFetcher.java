package fun.eightxm.wkit.generator;

/**
 * Abstracts Overpass API fetching for testability.
 * Default implementation POSTs to Overpass servers with fallback.
 * Tests can provide mock implementations with pre-built OSM JSON.
 */
@FunctionalInterface
public interface OverpassFetcher {

    /**
     * Fetch OSM data for the given bounding box as raw JSON string.
     *
     * @param query the Overpass QL query string
     * @return raw JSON response, or null if all servers fail
     */
    String fetch(String query);
}
