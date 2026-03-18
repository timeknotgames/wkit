package fun.eightxm.wkit.arnis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parses Arnis's metadata.json output to get MC coordinate bounds.
 */
public class ArnisMetadata {

    private int minMcX, maxMcX, minMcZ, maxMcZ;
    private double minGeoLat, maxGeoLat, minGeoLon, maxGeoLon;

    public static ArnisMetadata parse(Path metadataFile) throws IOException {
        Gson gson = new Gson();
        try (Reader reader = Files.newBufferedReader(metadataFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            ArnisMetadata meta = new ArnisMetadata();
            // Arnis uses camelCase keys: minMcX, maxMcX, minMcZ, maxMcZ, minGeoLat, etc.
            meta.minMcX = json.get("minMcX").getAsInt();
            meta.maxMcX = json.get("maxMcX").getAsInt();
            meta.minMcZ = json.get("minMcZ").getAsInt();
            meta.maxMcZ = json.get("maxMcZ").getAsInt();
            meta.minGeoLat = json.get("minGeoLat").getAsDouble();
            meta.maxGeoLat = json.get("maxGeoLat").getAsDouble();
            meta.minGeoLon = json.get("minGeoLon").getAsDouble();
            meta.maxGeoLon = json.get("maxGeoLon").getAsDouble();
            return meta;
        }
    }

    public int getWidthBlocks() { return maxMcX - minMcX; }
    public int getHeightBlocks() { return maxMcZ - minMcZ; }

    public int getMinMcX() { return minMcX; }
    public int getMaxMcX() { return maxMcX; }
    public int getMinMcZ() { return minMcZ; }
    public int getMaxMcZ() { return maxMcZ; }
    public double getMinGeoLat() { return minGeoLat; }
    public double getMaxGeoLat() { return maxGeoLat; }
    public double getMinGeoLon() { return minGeoLon; }
    public double getMaxGeoLon() { return maxGeoLon; }
}
