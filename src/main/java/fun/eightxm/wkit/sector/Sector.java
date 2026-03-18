package fun.eightxm.wkit.sector;

import java.util.UUID;

public class Sector {

    public enum Status {
        QUEUED, GENERATING, PLACING, LIVE, FAILED
    }

    private final String id;
    private String name;

    // Geographic bounds (real world)
    private double minLat, minLng, maxLat, maxLng;

    // Minecraft bounds (world coordinates, relative to Mount Tahoma origin)
    private int mcMinX, mcMinZ, mcMaxX, mcMaxZ;

    // Region-aligned offset applied during placement
    private int offsetX, offsetZ;

    private Status status;
    private long generatedAt;
    private String generatedBy; // Player UUID or "SYSTEM"

    // Generation parameters
    private double scale;
    private int groundLevel;
    private boolean terrain;

    // Path to saved OSM JSON data (relative to plugin data folder)
    private String osmDataFile;

    public Sector(String name, double minLat, double minLng, double maxLat, double maxLng) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.minLat = minLat;
        this.minLng = minLng;
        this.maxLat = maxLat;
        this.maxLng = maxLng;
        this.status = Status.QUEUED;
    }

    // For deserialization
    public Sector() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getMinLat() { return minLat; }
    public double getMinLng() { return minLng; }
    public double getMaxLat() { return maxLat; }
    public double getMaxLng() { return maxLng; }

    public int getMcMinX() { return mcMinX; }
    public int getMcMinZ() { return mcMinZ; }
    public int getMcMaxX() { return mcMaxX; }
    public int getMcMaxZ() { return mcMaxZ; }

    public void setMcBounds(int minX, int minZ, int maxX, int maxZ) {
        this.mcMinX = minX;
        this.mcMinZ = minZ;
        this.mcMaxX = maxX;
        this.mcMaxZ = maxZ;
    }

    public int getOffsetX() { return offsetX; }
    public int getOffsetZ() { return offsetZ; }
    public void setOffset(int offsetX, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }

    public int getGroundLevel() { return groundLevel; }
    public void setGroundLevel(int groundLevel) { this.groundLevel = groundLevel; }

    public boolean isTerrain() { return terrain; }
    public void setTerrain(boolean terrain) { this.terrain = terrain; }

    public String getOsmDataFile() { return osmDataFile; }
    public void setOsmDataFile(String osmDataFile) { this.osmDataFile = osmDataFile; }

    /**
     * Get the center of this sector in MC coordinates.
     */
    public int[] getMcCenter() {
        return new int[]{
            (mcMinX + mcMaxX) / 2,
            (mcMinZ + mcMaxZ) / 2
        };
    }

    /**
     * Check if a given MC coordinate falls within this sector.
     */
    public boolean containsMC(int x, int z) {
        return x >= mcMinX && x <= mcMaxX && z >= mcMinZ && z <= mcMaxZ;
    }

    @Override
    public String toString() {
        return name + " [" + id + "] (" + status + ") geo=["
            + minLat + "," + minLng + " → " + maxLat + "," + maxLng
            + "] mc=[" + mcMinX + "," + mcMinZ + " → " + mcMaxX + "," + mcMaxZ + "]";
    }
}
