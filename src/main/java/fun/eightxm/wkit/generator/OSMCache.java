package fun.eightxm.wkit.generator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Fetches and caches OpenStreetMap data from the Overpass API.
 * Data is cached per tile (~500m x 500m) and shared across chunks.
 * Thread-safe for concurrent chunk generation.
 */
public class OSMCache {

    private static final String[] OVERPASS_SERVERS = {
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://z.overpass-api.de/api/interpreter"
    };

    // Cache tile size in degrees (~500m at mid-latitudes)
    private static final double TILE_SIZE_DEG = 0.005;

    private final Path cacheDir;
    private final Logger logger;
    private final Gson gson = new Gson();

    // In-memory cache of parsed OSM elements per tile
    private final ConcurrentHashMap<String, List<OSMElement>> tileCache = new ConcurrentHashMap<>();

    // Track tiles currently being fetched to avoid duplicate requests
    private final Set<String> fetchingTiles = ConcurrentHashMap.newKeySet();

    public OSMCache(Path cacheDir, Logger logger) {
        this.cacheDir = cacheDir;
        this.logger = logger;
        try { Files.createDirectories(cacheDir); } catch (IOException ignored) {}
    }

    /**
     * Get all OSM elements that intersect with the given lat/lng bounding box.
     * Returns immediately with cached data, or empty list if not yet fetched.
     * Triggers async fetch for uncached tiles.
     */
    public List<OSMElement> getElements(double minLat, double minLng, double maxLat, double maxLng) {
        List<OSMElement> result = new ArrayList<>();

        // Find all tiles that overlap this bbox
        int tileMinX = (int) Math.floor(minLng / TILE_SIZE_DEG);
        int tileMinZ = (int) Math.floor(minLat / TILE_SIZE_DEG);
        int tileMaxX = (int) Math.floor(maxLng / TILE_SIZE_DEG);
        int tileMaxZ = (int) Math.floor(maxLat / TILE_SIZE_DEG);

        for (int tx = tileMinX; tx <= tileMaxX; tx++) {
            for (int tz = tileMinZ; tz <= tileMaxZ; tz++) {
                String key = tx + "_" + tz;
                List<OSMElement> cached = tileCache.get(key);
                if (cached != null) {
                    result.addAll(cached);
                } else {
                    // Trigger background fetch
                    triggerFetch(tx, tz);
                }
            }
        }

        return result;
    }

    private void triggerFetch(int tileX, int tileZ) {
        String key = tileX + "_" + tileZ;
        if (fetchingTiles.contains(key)) return;
        fetchingTiles.add(key);

        // Check disk cache first
        Path cacheFile = cacheDir.resolve("osm_" + key + ".json");
        if (Files.exists(cacheFile)) {
            try {
                List<OSMElement> elements = parseOSMFile(cacheFile);
                tileCache.put(key, elements);
                fetchingTiles.remove(key);
                return;
            } catch (Exception e) {
                logger.warning("Failed to read cached OSM: " + e.getMessage());
            }
        }

        // Fetch from Overpass API in background
        Thread fetcher = new Thread(() -> {
            try {
                double minLat = tileZ * TILE_SIZE_DEG;
                double minLng = tileX * TILE_SIZE_DEG;
                double maxLat = minLat + TILE_SIZE_DEG;
                double maxLng = minLng + TILE_SIZE_DEG;

                String query = buildOverpassQuery(minLat, minLng, maxLat, maxLng);
                String response = fetchOverpass(query);

                if (response != null) {
                    Files.writeString(cacheFile, response, StandardCharsets.UTF_8);
                    List<OSMElement> elements = parseOSMResponse(response);
                    tileCache.put(key, elements);
                    logger.info("Cached OSM tile " + key + ": " + elements.size() + " elements");
                } else {
                    tileCache.put(key, Collections.emptyList());
                }
            } catch (Exception e) {
                logger.warning("OSM fetch failed for tile " + key + ": " + e.getMessage());
                tileCache.put(key, Collections.emptyList());
            } finally {
                fetchingTiles.remove(key);
            }
        });
        fetcher.setDaemon(true);
        fetcher.setName("OSM-Fetch-" + key);
        fetcher.start();
    }

    private String buildOverpassQuery(double minLat, double minLng, double maxLat, double maxLng) {
        String bbox = minLat + "," + minLng + "," + maxLat + "," + maxLng;
        return "[out:json][timeout:30];\n" +
            "(\n" +
            "  way[\"building\"](" + bbox + ");\n" +
            "  way[\"highway\"](" + bbox + ");\n" +
            "  way[\"waterway\"](" + bbox + ");\n" +
            "  way[\"natural\"=\"water\"](" + bbox + ");\n" +
            "  way[\"landuse\"](" + bbox + ");\n" +
            "  way[\"natural\"=\"wood\"](" + bbox + ");\n" +
            "  way[\"natural\"=\"tree_row\"](" + bbox + ");\n" +
            "  way[\"leisure\"=\"park\"](" + bbox + ");\n" +
            "  relation[\"natural\"=\"water\"](" + bbox + ");\n" +
            ");\n" +
            "out body;\n" +
            ">;\n" +
            "out skel qt;";
    }

    private String fetchOverpass(String query) {
        for (String server : OVERPASS_SERVERS) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(server).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "WKit/1.0");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(("data=" + java.net.URLEncoder.encode(query, "UTF-8")).getBytes());
                }

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        return sb.toString();
                    }
                }
            } catch (Exception e) {
                // Try next server
            }
        }
        return null;
    }

    private List<OSMElement> parseOSMFile(Path file) throws IOException {
        return parseOSMResponse(Files.readString(file));
    }

    private List<OSMElement> parseOSMResponse(String json) {
        List<OSMElement> elements = new ArrayList<>();
        Map<Long, double[]> nodeMap = new HashMap<>();

        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonArray elems = root.getAsJsonArray("elements");
        if (elems == null) return elements;

        // First pass: collect all nodes
        for (JsonElement e : elems) {
            JsonObject obj = e.getAsJsonObject();
            if ("node".equals(obj.get("type").getAsString())) {
                long id = obj.get("id").getAsLong();
                double lat = obj.get("lat").getAsDouble();
                double lon = obj.get("lon").getAsDouble();
                nodeMap.put(id, new double[]{lat, lon});
            }
        }

        // Second pass: collect ways with resolved coordinates
        for (JsonElement e : elems) {
            JsonObject obj = e.getAsJsonObject();
            String type = obj.get("type").getAsString();
            if (!"way".equals(type)) continue;

            JsonObject tags = obj.has("tags") ? obj.getAsJsonObject("tags") : null;
            if (tags == null) continue;

            JsonArray nodes = obj.getAsJsonArray("nodes");
            if (nodes == null) continue;

            List<double[]> coords = new ArrayList<>();
            for (JsonElement n : nodes) {
                double[] coord = nodeMap.get(n.getAsLong());
                if (coord != null) coords.add(coord);
            }

            if (coords.isEmpty()) continue;

            OSMElement elem = new OSMElement();
            elem.coordinates = coords;

            // Classify the element
            if (tags.has("building")) {
                elem.type = OSMElement.Type.BUILDING;
                elem.subtype = tags.get("building").getAsString();
            } else if (tags.has("highway")) {
                elem.type = OSMElement.Type.HIGHWAY;
                elem.subtype = tags.get("highway").getAsString();
            } else if (tags.has("waterway")) {
                elem.type = OSMElement.Type.WATERWAY;
                elem.subtype = tags.get("waterway").getAsString();
            } else if (tags.has("natural") && tags.get("natural").getAsString().equals("water")) {
                elem.type = OSMElement.Type.WATER;
            } else if (tags.has("natural") && tags.get("natural").getAsString().equals("wood")) {
                elem.type = OSMElement.Type.FOREST;
            } else if (tags.has("landuse")) {
                elem.type = OSMElement.Type.LANDUSE;
                elem.subtype = tags.get("landuse").getAsString();
            } else if (tags.has("leisure") && tags.get("leisure").getAsString().equals("park")) {
                elem.type = OSMElement.Type.PARK;
            } else {
                continue;
            }

            if (tags.has("name")) {
                elem.name = tags.get("name").getAsString();
            }

            elements.add(elem);
        }

        return elements;
    }

    /**
     * Represents a parsed OSM element with coordinates and classification.
     */
    public static class OSMElement {
        public enum Type {
            BUILDING, HIGHWAY, WATERWAY, WATER, FOREST, LANDUSE, PARK
        }

        public Type type;
        public String subtype; // e.g., "residential" for building, "primary" for highway
        public String name;
        public List<double[]> coordinates; // list of [lat, lng] pairs
    }
}
