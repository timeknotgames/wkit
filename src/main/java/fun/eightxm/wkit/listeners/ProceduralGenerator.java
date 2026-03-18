package fun.eightxm.wkit.listeners;

import fun.eightxm.wkit.WKitPlugin;
import fun.eightxm.wkit.arnis.ArnisCLI;
import fun.eightxm.wkit.arnis.GenerationTask;
import fun.eightxm.wkit.region.RegionMath;
import fun.eightxm.wkit.sector.GeoProjection;
import fun.eightxm.wkit.sector.Sector;
import fun.eightxm.wkit.sector.SectorPlacement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procedural live generation: when a player approaches ungenerated terrain,
 * automatically triggers Arnis to generate it region-by-region.
 *
 * Detects when a player is within TRIGGER_DISTANCE blocks of an ungenerated region,
 * queues generation for that region cluster, and shows a loading message.
 */
public class ProceduralGenerator implements Listener {

    /** How close (blocks) a player must be to a region edge to trigger generation. */
    private static final int TRIGGER_DISTANCE = 256; // half a region

    /** How many regions to generate around the trigger point. */
    private static final int CLUSTER_RADIUS = 1; // 3x3 regions = ~1.5km x 1.5km

    /** Minimum seconds between generation checks per player. */
    private static final long CHECK_COOLDOWN_MS = 5000;

    private final WKitPlugin plugin;
    private final SectorPlacement placement;

    /** Regions currently being generated or queued (regionX,regionZ string keys). */
    private final Set<String> pendingRegions = ConcurrentHashMap.newKeySet();

    /** Last check time per player to avoid excessive computation. */
    private final ConcurrentHashMap<String, Long> lastCheckTime = new ConcurrentHashMap<>();

    private boolean enabled = true;

    public ProceduralGenerator(WKitPlugin plugin) {
        this.plugin = plugin;
        this.placement = new SectorPlacement(plugin.getGeoProjection());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled) return;

        // Only check on block changes (not sub-block movement)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();

        // Cooldown check
        long now = System.currentTimeMillis();
        Long lastCheck = lastCheckTime.get(playerId);
        if (lastCheck != null && (now - lastCheck) < CHECK_COOLDOWN_MS) {
            return;
        }
        lastCheckTime.put(playerId, now);

        // Only check in the main world
        if (!player.getWorld().getName().equals(plugin.getWorldName())) {
            return;
        }

        Location loc = player.getLocation();
        int px = loc.getBlockX();
        int pz = loc.getBlockZ();

        // Check the region the player is in, and adjacent regions within trigger distance
        checkAndGenerate(player, px, pz);
        checkAndGenerate(player, px + TRIGGER_DISTANCE, pz);
        checkAndGenerate(player, px - TRIGGER_DISTANCE, pz);
        checkAndGenerate(player, px, pz + TRIGGER_DISTANCE);
        checkAndGenerate(player, px, pz - TRIGGER_DISTANCE);
    }

    private void checkAndGenerate(Player player, int mcX, int mcZ) {
        int regionX = RegionMath.blockToRegion(mcX);
        int regionZ = RegionMath.blockToRegion(mcZ);
        String regionKey = regionX + "," + regionZ;

        // Skip if this region is already generated (part of a live sector)
        if (plugin.getSectorRegistry().findByCoords(
                regionX * RegionMath.REGION_SIZE + 256,
                regionZ * RegionMath.REGION_SIZE + 256) != null) {
            return;
        }

        // Skip if already pending
        if (pendingRegions.contains(regionKey)) {
            return;
        }

        // Check if Arnis binary is available
        ArnisCLI arnis = new ArnisCLI(plugin.getArnisBinaryPath(), plugin.getLogger());
        if (!arnis.isAvailable()) {
            return;
        }

        // Mark as pending (mark the whole cluster)
        SectorPlacement.RegionAlignedBBox cluster = placement.regionCluster(mcX, mcZ, CLUSTER_RADIUS);
        for (int rx = cluster.regionMinX; rx <= cluster.regionMaxX; rx++) {
            for (int rz = cluster.regionMinZ; rz <= cluster.regionMaxZ; rz++) {
                pendingRegions.add(rx + "," + rz);
            }
        }

        // Show loading message
        GeoProjection proj = plugin.getGeoProjection();
        double[] latLng = proj.toLatlng(mcX, mcZ);
        String areaName = String.format("%.4f,%.4f", latLng[0], latLng[1]);

        player.showTitle(Title.title(
            Component.text("Generating Terrain", NamedTextColor.GOLD),
            Component.text("Loading " + areaName + " (" + cluster + ")", NamedTextColor.YELLOW),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));

        plugin.getLogger().info("[ProceduralGen] Triggering generation at " + cluster
            + " for player " + player.getName());

        // Create sector and generate
        String sectorName = "auto_" + regionX + "_" + regionZ;
        Sector sector = new Sector(sectorName,
            cluster.geoMinLat, cluster.geoMinLng, cluster.geoMaxLat, cluster.geoMaxLng);
        sector.setScale(plugin.getDefaultScale());
        sector.setGroundLevel(plugin.getDefaultGroundLevel());
        sector.setTerrain(plugin.getDefaultTerrain());

        // Set pre-computed MC bounds (already region-aligned)
        sector.setMcBounds(cluster.mcMinX, cluster.mcMinZ, cluster.mcMaxX, cluster.mcMaxZ);
        sector.setOffset(cluster.mcMinX, cluster.mcMinZ);

        GenerationTask task = new GenerationTask(plugin, sector, player.getUniqueId()) {
            @Override
            public void run() {
                try {
                    super.run();
                } finally {
                    // Clear pending flags when done (success or failure)
                    for (int rx = cluster.regionMinX; rx <= cluster.regionMaxX; rx++) {
                        for (int rz = cluster.regionMinZ; rz <= cluster.regionMaxZ; rz++) {
                            pendingRegions.remove(rx + "," + rz);
                        }
                    }
                }
            }
        };

        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    /** Get the number of regions currently being generated. */
    public int getPendingCount() {
        return pendingRegions.size();
    }
}
