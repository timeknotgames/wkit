package fun.eightxm.wkit.arnis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArnisMetadata — parsing Arnis CLI output JSON.
 */
class ArnisMetadataTest {

    @TempDir
    Path tempDir;

    @Test
    void parseValidMetadata() throws IOException {
        Path file = tempDir.resolve("metadata.json");
        Files.writeString(file, """
            {
                "minMcX": -256,
                "maxMcX": 512,
                "minMcZ": -128,
                "maxMcZ": 384,
                "minGeoLat": 47.252,
                "maxGeoLat": 47.258,
                "minGeoLon": -122.450,
                "maxGeoLon": -122.438
            }
            """);

        ArnisMetadata meta = ArnisMetadata.parse(file);

        assertEquals(-256, meta.getMinMcX());
        assertEquals(512, meta.getMaxMcX());
        assertEquals(-128, meta.getMinMcZ());
        assertEquals(384, meta.getMaxMcZ());
        assertEquals(47.252, meta.getMinGeoLat(), 0.001);
        assertEquals(47.258, meta.getMaxGeoLat(), 0.001);
        assertEquals(-122.450, meta.getMinGeoLon(), 0.001);
        assertEquals(-122.438, meta.getMaxGeoLon(), 0.001);
    }

    @Test
    void widthAndHeightCalculation() throws IOException {
        Path file = tempDir.resolve("metadata.json");
        Files.writeString(file, """
            {
                "minMcX": -256, "maxMcX": 512,
                "minMcZ": -128, "maxMcZ": 384,
                "minGeoLat": 0, "maxGeoLat": 1,
                "minGeoLon": 0, "maxGeoLon": 1
            }
            """);

        ArnisMetadata meta = ArnisMetadata.parse(file);
        assertEquals(768, meta.getWidthBlocks());   // 512 - (-256) = 768
        assertEquals(512, meta.getHeightBlocks());   // 384 - (-128) = 512
    }

    @Test
    void parseNonExistentFileThrows() {
        Path file = tempDir.resolve("nonexistent.json");
        assertThrows(IOException.class, () -> ArnisMetadata.parse(file));
    }

    @Test
    void parseMalformedJsonThrows() throws IOException {
        Path file = tempDir.resolve("bad.json");
        Files.writeString(file, "not valid json");
        assertThrows(Exception.class, () -> ArnisMetadata.parse(file));
    }

    @Test
    void parseMissingFieldThrows() throws IOException {
        Path file = tempDir.resolve("incomplete.json");
        Files.writeString(file, """
            {
                "minMcX": -256
            }
            """);
        assertThrows(Exception.class, () -> ArnisMetadata.parse(file));
    }
}
