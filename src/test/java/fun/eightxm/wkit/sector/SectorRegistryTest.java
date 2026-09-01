package fun.eightxm.wkit.sector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SectorRegistry — persistence, lookup, overlap detection.
 * Uses @TempDir for file I/O isolation.
 */
class SectorRegistryTest {

    @TempDir
    Path tempDir;

    private File dataFile;
    private SectorRegistry registry;

    @BeforeEach
    void setUp() {
        dataFile = tempDir.resolve("sectors.json").toFile();
        registry = new SectorRegistry(dataFile);
    }

    // === Registration ===

    @Test
    void registerAndFindById() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        registry.register(s);

        assertNotNull(registry.findById(s.getId()));
        assertEquals("Tacoma", registry.findById(s.getId()).getName());
    }

    @Test
    void registerPersistsToFile() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        registry.register(s);

        assertTrue(dataFile.exists(), "Data file should be created after register");
        assertTrue(dataFile.length() > 0, "Data file should not be empty");
    }

    // === Save/Load Round-trip ===

    @Test
    void saveAndLoadRoundTrip() {
        Sector s1 = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        s1.setMcBounds(-100, -200, 300, 400);
        s1.setStatus(Sector.Status.LIVE);
        s1.setScale(1.0);
        registry.register(s1);

        Sector s2 = new Sector("Seattle", 47.600, -122.340, 47.610, -122.320);
        s2.setMcBounds(-500, -600, -100, -200);
        s2.setStatus(Sector.Status.GENERATING);
        registry.register(s2);

        // Load into fresh registry
        SectorRegistry loaded = new SectorRegistry(dataFile);
        loaded.load();

        assertEquals(2, loaded.getAll().size());
        assertNotNull(loaded.findById(s1.getId()));
        assertNotNull(loaded.findById(s2.getId()));
    }

    @Test
    void loadNonExistentFileDoesNotCrash() {
        File missing = tempDir.resolve("nonexistent.json").toFile();
        SectorRegistry r = new SectorRegistry(missing);
        r.load(); // should not throw
        assertEquals(0, r.getAll().size());
    }

    // === Find Methods ===

    @Test
    void findByName() {
        Sector s = new Sector("Downtown Tacoma", 47.252, -122.450, 47.258, -122.438);
        registry.register(s);

        assertNotNull(registry.findByName("Downtown Tacoma"));
        assertNotNull(registry.findByName("downtown tacoma")); // case-insensitive
        assertNull(registry.findByName("Seattle"));
    }

    @Test
    void findByNameOrId() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        registry.register(s);

        assertNotNull(registry.findByNameOrId(s.getId()));
        assertNotNull(registry.findByNameOrId("Tacoma"));
        assertNull(registry.findByNameOrId("nonexistent"));
    }

    @Test
    void findByCoords() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        s.setMcBounds(0, 0, 100, 100);
        registry.register(s);

        assertNotNull(registry.findByCoords(50, 50));
        assertNull(registry.findByCoords(200, 200));
    }

    // === Overlap Detection ===

    @Test
    void checkOverlapWithLiveSector() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        s.setMcBounds(0, 0, 100, 100);
        s.setStatus(Sector.Status.LIVE);
        registry.register(s);

        assertTrue(registry.checkOverlap(50, 50, 150, 150), "Overlapping box should be detected");
        assertTrue(registry.checkOverlap(0, 0, 100, 100), "Exact match should overlap");
        assertFalse(registry.checkOverlap(101, 101, 200, 200), "Non-overlapping should be false");
    }

    @Test
    void checkOverlapIgnoresNonLiveSectors() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        s.setMcBounds(0, 0, 100, 100);
        s.setStatus(Sector.Status.QUEUED);
        registry.register(s);

        assertFalse(registry.checkOverlap(50, 50, 150, 150),
            "QUEUED sectors should not trigger overlap");
    }

    @Test
    void checkOverlapWithNegativeCoords() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        s.setMcBounds(-500, -500, -100, -100);
        s.setStatus(Sector.Status.LIVE);
        registry.register(s);

        assertTrue(registry.checkOverlap(-300, -300, -200, -200));
        assertFalse(registry.checkOverlap(0, 0, 100, 100));
    }

    // === Remove ===

    @Test
    void removeSector() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        registry.register(s);
        assertEquals(1, registry.getAll().size());

        registry.remove(s.getId());
        assertEquals(0, registry.getAll().size());
        assertNull(registry.findById(s.getId()));
    }

    // === Atomic Write Safety ===

    @Test
    void noTmpFileLeftAfterSave() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        registry.register(s);

        File tmpFile = new File(dataFile.getParentFile(), dataFile.getName() + ".tmp");
        assertFalse(tmpFile.exists(), "Temp file should be cleaned up after atomic rename");
    }

    @Test
    void multipleRegistrationsProduceValidFile() {
        for (int i = 0; i < 10; i++) {
            Sector s = new Sector("Sector-" + i, 47.0 + i * 0.01, -122.0, 47.01 + i * 0.01, -121.99);
            registry.register(s);
        }

        SectorRegistry loaded = new SectorRegistry(dataFile);
        loaded.load();
        assertEquals(10, loaded.getAll().size());
    }
}
