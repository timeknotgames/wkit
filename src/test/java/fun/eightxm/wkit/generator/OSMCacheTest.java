package fun.eightxm.wkit.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OSMCache — parsing, element classification, tile math.
 * Uses mock OverpassFetcher to avoid HTTP calls.
 */
class OSMCacheTest {

    @TempDir
    Path tempDir;

    private OSMCache cache;

    @BeforeEach
    void setUp() {
        cache = new OSMCache(tempDir, Logger.getLogger("test"));
    }

    // === parseOSMResponse ===

    @Test
    void parseBuilding() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.253,"lon":-122.441},
                    {"type":"node","id":3,"lat":47.254,"lon":-122.441},
                    {"type":"node","id":4,"lat":47.254,"lon":-122.442},
                    {"type":"way","id":100,"nodes":[1,2,3,4,1],
                     "tags":{"building":"residential","name":"Test House"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(1, elements.size());

        OSMCache.OSMElement elem = elements.get(0);
        assertEquals(OSMCache.OSMElement.Type.BUILDING, elem.type);
        assertEquals("residential", elem.subtype);
        assertEquals("Test House", elem.name);
        assertEquals(5, elem.coordinates.size()); // 4 unique + closing node repeated
    }

    @Test
    void parseHighway() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":200,"nodes":[1,2],
                     "tags":{"highway":"primary","name":"Pacific Ave"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(1, elements.size());
        assertEquals(OSMCache.OSMElement.Type.HIGHWAY, elements.get(0).type);
        assertEquals("primary", elements.get(0).subtype);
        assertEquals("Pacific Ave", elements.get(0).name);
    }

    @Test
    void parseWaterway() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":300,"nodes":[1,2],
                     "tags":{"waterway":"river","name":"Puyallup River"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(1, elements.size());
        assertEquals(OSMCache.OSMElement.Type.WATERWAY, elements.get(0).type);
    }

    @Test
    void parseNaturalWater() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":400,"nodes":[1,2],
                     "tags":{"natural":"water"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(1, elements.size());
        assertEquals(OSMCache.OSMElement.Type.WATER, elements.get(0).type);
    }

    @Test
    void parseForest() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":500,"nodes":[1,2],
                     "tags":{"natural":"wood"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(1, elements.size());
        assertEquals(OSMCache.OSMElement.Type.FOREST, elements.get(0).type);
    }

    @Test
    void parsePark() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":600,"nodes":[1,2],
                     "tags":{"leisure":"park","name":"Wright Park"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(1, elements.size());
        assertEquals(OSMCache.OSMElement.Type.PARK, elements.get(0).type);
        assertEquals("Wright Park", elements.get(0).name);
    }

    @Test
    void parseLanduse() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":700,"nodes":[1,2],
                     "tags":{"landuse":"industrial"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(1, elements.size());
        assertEquals(OSMCache.OSMElement.Type.LANDUSE, elements.get(0).type);
        assertEquals("industrial", elements.get(0).subtype);
    }

    @Test
    void parseSkipsWaysWithoutTags() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":800,"nodes":[1,2]}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(0, elements.size(), "Ways without tags should be skipped");
    }

    @Test
    void parseSkipsWaysWithUnrecognizedTags() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"way","id":900,"nodes":[1,2],
                     "tags":{"amenity":"restaurant"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(0, elements.size(), "Unrecognized tags should be skipped");
    }

    @Test
    void parseMixedElements() {
        String json = """
            {
                "elements": [
                    {"type":"node","id":1,"lat":47.253,"lon":-122.442},
                    {"type":"node","id":2,"lat":47.254,"lon":-122.441},
                    {"type":"node","id":3,"lat":47.255,"lon":-122.440},
                    {"type":"way","id":100,"nodes":[1,2],
                     "tags":{"building":"apartments","name":"Tower A"}},
                    {"type":"way","id":200,"nodes":[2,3],
                     "tags":{"highway":"residential","name":"N 21st St"}},
                    {"type":"way","id":300,"nodes":[1,3],
                     "tags":{"natural":"water"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(3, elements.size());
    }

    @Test
    void parseEmptyElements() {
        String json = """
            {"elements": []}
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(0, elements.size());
    }

    @Test
    void parseMissingNodesHandledGracefully() {
        String json = """
            {
                "elements": [
                    {"type":"way","id":100,"nodes":[999,998],
                     "tags":{"building":"yes"}}
                ]
            }
            """;

        List<OSMCache.OSMElement> elements = cache.parseOSMResponse(json);
        assertEquals(0, elements.size(), "Ways with unresolved nodes should be skipped");
    }

    // === Tile Key Math ===

    @Test
    void tileSizeDegrees() {
        assertEquals(0.005, OSMCache.TILE_SIZE_DEG, "Tile size should be 0.005 degrees");
    }

    // === buildOverpassQuery ===

    @Test
    void overpassQueryContainsBbox() {
        String query = cache.buildOverpassQuery(47.252, -122.450, 47.258, -122.438);
        assertTrue(query.contains("47.252"), "Query should contain min lat");
        assertTrue(query.contains("-122.45"), "Query should contain min lng");
        assertTrue(query.contains("building"), "Query should fetch buildings");
        assertTrue(query.contains("highway"), "Query should fetch highways");
        assertTrue(query.contains("waterway"), "Query should fetch waterways");
    }
}
