package fun.eightxm.wkit.sector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Sector — data model with lifecycle and geographic/MC bounds.
 */
class SectorTest {

    // === Construction ===

    @Test
    void constructorSetsFields() {
        Sector s = new Sector("Downtown Tacoma", 47.252, -122.450, 47.258, -122.438);
        assertEquals("Downtown Tacoma", s.getName());
        assertEquals(47.252, s.getMinLat());
        assertEquals(-122.450, s.getMinLng());
        assertEquals(47.258, s.getMaxLat());
        assertEquals(-122.438, s.getMaxLng());
    }

    @Test
    void constructorGeneratesId() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        assertNotNull(s.getId());
        assertEquals(8, s.getId().length(), "ID should be 8 chars (UUID prefix)");
    }

    @Test
    void idsAreUnique() {
        Sector s1 = new Sector("A", 0, 0, 1, 1);
        Sector s2 = new Sector("B", 0, 0, 1, 1);
        assertNotEquals(s1.getId(), s2.getId());
    }

    @Test
    void initialStatusIsQueued() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        assertEquals(Sector.Status.QUEUED, s.getStatus());
    }

    // === Status Lifecycle ===

    @Test
    void statusTransitionToLive() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setStatus(Sector.Status.GENERATING);
        assertEquals(Sector.Status.GENERATING, s.getStatus());
        s.setStatus(Sector.Status.PLACING);
        assertEquals(Sector.Status.PLACING, s.getStatus());
        s.setStatus(Sector.Status.LIVE);
        assertEquals(Sector.Status.LIVE, s.getStatus());
    }

    @Test
    void statusTransitionToFailed() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setStatus(Sector.Status.GENERATING);
        s.setStatus(Sector.Status.FAILED);
        assertEquals(Sector.Status.FAILED, s.getStatus());
    }

    // === MC Bounds ===

    @Test
    void setMcBounds() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setMcBounds(-100, -200, 300, 400);
        assertEquals(-100, s.getMcMinX());
        assertEquals(-200, s.getMcMinZ());
        assertEquals(300, s.getMcMaxX());
        assertEquals(400, s.getMcMaxZ());
    }

    // === containsMC ===

    @Test
    void containsMcInside() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setMcBounds(0, 0, 100, 100);
        assertTrue(s.containsMC(50, 50));
    }

    @Test
    void containsMcOnBoundary() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setMcBounds(0, 0, 100, 100);
        assertTrue(s.containsMC(0, 0), "Min boundary should be inclusive");
        assertTrue(s.containsMC(100, 100), "Max boundary should be inclusive");
    }

    @Test
    void containsMcOutside() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setMcBounds(0, 0, 100, 100);
        assertFalse(s.containsMC(-1, 50));
        assertFalse(s.containsMC(101, 50));
        assertFalse(s.containsMC(50, -1));
        assertFalse(s.containsMC(50, 101));
    }

    @Test
    void containsMcWithNegativeBounds() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setMcBounds(-500, -500, -100, -100);
        assertTrue(s.containsMC(-300, -300));
        assertFalse(s.containsMC(0, 0));
        assertTrue(s.containsMC(-500, -500));
        assertTrue(s.containsMC(-100, -100));
    }

    // === getMcCenter ===

    @Test
    void getMcCenterPositive() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setMcBounds(0, 0, 100, 200);
        int[] center = s.getMcCenter();
        assertEquals(50, center[0]);
        assertEquals(100, center[1]);
    }

    @Test
    void getMcCenterNegative() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setMcBounds(-500, -500, -100, -100);
        int[] center = s.getMcCenter();
        assertEquals(-300, center[0]);
        assertEquals(-300, center[1]);
    }

    // === Generation Metadata ===

    @Test
    void generationParameters() {
        Sector s = new Sector("Test", 0, 0, 1, 1);
        s.setScale(2.0);
        s.setGroundLevel(-62);
        s.setTerrain(true);
        s.setGeneratedBy("abc-123");
        s.setGeneratedAt(1234567890L);

        assertEquals(2.0, s.getScale());
        assertEquals(-62, s.getGroundLevel());
        assertTrue(s.isTerrain());
        assertEquals("abc-123", s.getGeneratedBy());
        assertEquals(1234567890L, s.getGeneratedAt());
    }

    // === toString ===

    @Test
    void toStringContainsKeyInfo() {
        Sector s = new Sector("Tacoma", 47.252, -122.450, 47.258, -122.438);
        s.setMcBounds(-100, -200, 300, 400);
        s.setStatus(Sector.Status.LIVE);
        String str = s.toString();
        assertTrue(str.contains("Tacoma"));
        assertTrue(str.contains("LIVE"));
    }
}
