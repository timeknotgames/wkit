package fun.eightxm.wkit;

import fun.eightxm.wkit.commands.SectorCommand;
import fun.eightxm.wkit.commands.WKitCommand;
import fun.eightxm.wkit.generator.ArnisChunkGenerator;
import fun.eightxm.wkit.listeners.ProceduralGenerator;
import fun.eightxm.wkit.sector.GeoProjection;
import fun.eightxm.wkit.sector.SectorRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;

public class WKitPlugin extends JavaPlugin {

    private static WKitPlugin instance;
    private SectorRegistry sectorRegistry;
    private GeoProjection geoProjection;
    private ArnisChunkGenerator chunkGenerator;
    private ProceduralGenerator proceduralGenerator;
    private Path arnisBinaryPath;
    private Path tempDirectory;

    private static final String ARNIS_WORLD_NAME = "arnis_world";

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Load config
        arnisBinaryPath = Path.of(getConfig().getString("arnis.binary-path", "/home/zufaydi/mc/arnis-main/target/release/arnis"));
        tempDirectory = Path.of(getConfig().getString("arnis.temp-directory", "/tmp/wkit-gen"));

        // Validate config
        if (!java.nio.file.Files.isExecutable(arnisBinaryPath)) {
            getLogger().warning("Arnis binary not found or not executable: " + arnisBinaryPath);
            getLogger().warning("Sector generation via Arnis CLI will be unavailable.");
        }
        if (!java.nio.file.Files.isDirectory(tempDirectory)) {
            try {
                java.nio.file.Files.createDirectories(tempDirectory);
            } catch (java.io.IOException e) {
                getLogger().warning("Could not create temp directory: " + tempDirectory);
            }
        }

        // Initialize geo projection from Mount Tahoma origin
        double originLat = getConfig().getDouble("projection.origin-lat", 46.8523);
        double originLng = getConfig().getDouble("projection.origin-lng", -121.7603);
        double scale = getConfig().getDouble("projection.scale", 1.0);

        if (originLat < -90 || originLat > 90 || originLng < -180 || originLng > 180) {
            getLogger().severe("Invalid projection origin: " + originLat + ", " + originLng + " — using Tahoma defaults");
            originLat = 46.8523;
            originLng = -121.7603;
        }
        if (scale <= 0) {
            getLogger().severe("Invalid projection scale: " + scale + " — using 1.0");
            scale = 1.0;
        }

        geoProjection = new GeoProjection(originLat, originLng, scale);

        // Create custom chunk generator
        Path elevCacheDir = getDataFolder().toPath().resolve("elevation-cache");
        chunkGenerator = new ArnisChunkGenerator(geoProjection, elevCacheDir, getLogger());

        // Create or load the Arnis world with our custom generator
        createArnisWorld();

        // Initialize sector registry
        File dataFile = new File(getDataFolder(), "sectors.json");
        sectorRegistry = new SectorRegistry(dataFile);
        sectorRegistry.load();

        // Register commands
        getCommand("sector").setExecutor(new SectorCommand(this));
        getCommand("wkit").setExecutor(new WKitCommand(this));

        // Register procedural generation listener (disabled by default now — ChunkGenerator handles it)
        proceduralGenerator = new ProceduralGenerator(this);
        proceduralGenerator.setEnabled(false);
        getServer().getPluginManager().registerEvents(proceduralGenerator, this);

        // Auto-teleport players to arnis_world on join
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                World arnisWorld = Bukkit.getWorld(ARNIS_WORLD_NAME);
                if (arnisWorld == null) return;
                if (!event.getPlayer().getWorld().getName().equals(ARNIS_WORLD_NAME)) {
                    // Teleport to configured spawn (South Tacoma default)
                    double spawnLat = getConfig().getDouble("spawn.lat", 47.2070);
                    double spawnLng = getConfig().getDouble("spawn.lng", -122.4590);
                    int[] mc = geoProjection.toMC(spawnLat, spawnLng);
                    Location spawn = new Location(arnisWorld, mc[0], 100, mc[1]); // Y=100 for safe landing
                    event.getPlayer().teleport(spawn);
                    event.getPlayer().sendMessage("[WKit] Welcome to the Worlding Kit! Generating terrain...");
                }
            }
        }, this);

        getLogger().info("WKit enabled! Origin: Mount Tahoma (" + originLat + ", " + originLng + ")");
        getLogger().info("Arnis binary: " + arnisBinaryPath);
        getLogger().info("Arnis world: " + ARNIS_WORLD_NAME + " with custom ChunkGenerator");
        getLogger().info("Sectors loaded: " + sectorRegistry.getAll().size());
    }

    private void createArnisWorld() {
        World existing = Bukkit.getWorld(ARNIS_WORLD_NAME);
        if (existing != null) {
            getLogger().info("Arnis world already loaded: " + ARNIS_WORLD_NAME);
            return;
        }

        getLogger().info("Creating Arnis world with real-terrain ChunkGenerator...");
        WorldCreator creator = new WorldCreator(ARNIS_WORLD_NAME);
        creator.generator(chunkGenerator);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);

        World world = creator.createWorld();
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            getLogger().info("Arnis world created! Players will see real elevation terrain.");
        } else {
            getLogger().severe("Failed to create Arnis world!");
        }
    }

    @Override
    public void onDisable() {
        // Shut down OSM fetch thread pool
        if (chunkGenerator != null && chunkGenerator.getOsmCache() != null) {
            try {
                chunkGenerator.getOsmCache().shutdown();
                getLogger().info("OSM fetch threads shut down.");
            } catch (Exception e) {
                getLogger().warning("Error shutting down OSM cache: " + e.getMessage());
            }
        }

        // Save sector registry
        if (sectorRegistry != null) {
            try {
                sectorRegistry.save();
                getLogger().info("Sector registry saved (" + sectorRegistry.getAll().size() + " sectors).");
            } catch (Exception e) {
                getLogger().warning("Error saving sector registry: " + e.getMessage());
            }
        }

        getLogger().info("WKit disabled.");
    }

    /**
     * Paper calls this to resolve generator names from bukkit.yml.
     * Allows: generator: WKit in bukkit.yml
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return chunkGenerator;
    }

    public static WKitPlugin getInstance() { return instance; }
    public SectorRegistry getSectorRegistry() { return sectorRegistry; }
    public GeoProjection getGeoProjection() { return geoProjection; }
    public ArnisChunkGenerator getChunkGenerator() { return chunkGenerator; }
    public Path getArnisBinaryPath() { return arnisBinaryPath; }
    public Path getTempDirectory() { return tempDirectory; }

    public String getWorldName() {
        return ARNIS_WORLD_NAME;
    }

    public double getDefaultScale() {
        return getConfig().getDouble("arnis.default-scale", 1.0);
    }

    public int getDefaultGroundLevel() {
        return getConfig().getInt("arnis.default-ground-level", -62);
    }

    public boolean getDefaultTerrain() {
        return getConfig().getBoolean("arnis.default-terrain", true);
    }

    public int getGenerationTimeout() {
        return getConfig().getInt("arnis.generation-timeout-seconds", 600);
    }

    public ProceduralGenerator getProceduralGenerator() {
        return proceduralGenerator;
    }
}
