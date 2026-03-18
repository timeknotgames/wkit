package fun.eightxm.wkit.arnis;

import fun.eightxm.wkit.WKitPlugin;
import fun.eightxm.wkit.region.RegionFileManager;
import fun.eightxm.wkit.region.RegionMath;
import fun.eightxm.wkit.sector.GeoProjection;
import fun.eightxm.wkit.sector.Sector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Runs the full sector generation pipeline asynchronously.
 * 1. Call Arnis CLI
 * 2. Read metadata
 * 3. Compute geographic offset
 * 4. Relocate region files
 * 5. Register sector
 */
public class GenerationTask implements Runnable {

    private final WKitPlugin plugin;
    private final Sector sector;
    private final UUID requestedBy; // Player UUID, or null for console

    public GenerationTask(WKitPlugin plugin, Sector sector, UUID requestedBy) {
        this.plugin = plugin;
        this.sector = sector;
        this.requestedBy = requestedBy;
    }

    @Override
    public void run() {
        Logger logger = plugin.getLogger();
        GeoProjection proj = plugin.getGeoProjection();

        sector.setStatus(Sector.Status.GENERATING);
        sendProgress("Generating sector '" + sector.getName() + "'...");

        try {
            // Create temp directory for this generation
            Path tempDir = plugin.getTempDirectory().resolve("gen-" + sector.getId());
            Files.createDirectories(tempDir);

            // Build bbox string
            String bbox = sector.getMinLat() + "," + sector.getMinLng() + ","
                        + sector.getMaxLat() + "," + sector.getMaxLng();

            // OSM data save path
            Path osmJsonPath = plugin.getDataFolder().toPath().resolve("osm-data");
            Files.createDirectories(osmJsonPath);
            Path osmFile = osmJsonPath.resolve(sector.getId() + ".json");

            // Run Arnis
            ArnisCLI arnis = new ArnisCLI(plugin.getArnisBinaryPath(), logger);
            Path worldDir = arnis.generate(
                bbox, tempDir,
                sector.isTerrain(), sector.getScale(), sector.getGroundLevel(),
                osmFile, plugin.getGenerationTimeout(),
                line -> sendProgress("[Arnis] " + line)
            );

            sector.setOsmDataFile("osm-data/" + sector.getId() + ".json");

            // Read metadata
            Path metadataFile = worldDir.resolve("metadata.json");
            ArnisMetadata metadata;
            if (Files.exists(metadataFile)) {
                metadata = ArnisMetadata.parse(metadataFile);
                logger.info("Arnis generated: " + metadata.getWidthBlocks() + "x" + metadata.getHeightBlocks() + " blocks");
            } else {
                logger.warning("No metadata.json found, estimating bounds from projection");
                metadata = null;
            }

            // Compute target MC position using geographic projection
            int[] targetMcMin = proj.toMC(sector.getMinLat(), sector.getMinLng());
            int[] targetMcMax = proj.toMC(sector.getMaxLat(), sector.getMaxLng());
            int mcMinX = Math.min(targetMcMin[0], targetMcMax[0]);
            int mcMinZ = Math.min(targetMcMin[1], targetMcMax[1]);
            int mcMaxX = Math.max(targetMcMin[0], targetMcMax[0]);
            int mcMaxZ = Math.max(targetMcMin[1], targetMcMax[1]);

            // Snap to region boundaries
            int snappedMinX = GeoProjection.snapToRegion(mcMinX);
            int snappedMinZ = GeoProjection.snapToRegion(mcMinZ);

            sector.setMcBounds(mcMinX, mcMinZ, mcMaxX, mcMaxZ);
            sector.setOffset(snappedMinX, snappedMinZ);

            // Check overlap
            if (plugin.getSectorRegistry().checkOverlap(mcMinX, mcMinZ, mcMaxX, mcMaxZ)) {
                sector.setStatus(Sector.Status.FAILED);
                sendProgress("FAILED: Sector overlaps with existing sector!");
                return;
            }

            // Paste terrain via FAWE (async block placement through Bukkit API)
            // This avoids the NBT format mismatch that breaks direct .mca file injection
            sector.setStatus(Sector.Status.PLACING);
            sendProgress("Pasting terrain via FAWE (" + metadata.getWidthBlocks() + "x" + metadata.getHeightBlocks() + " blocks)...");

            World world = Bukkit.getWorld(plugin.getWorldName());
            if (world == null) {
                throw new IOException("World not found: " + plugin.getWorldName());
            }

            // Arnis generates at origin (0,0). We paste at the geographic offset.
            FawePaster paster = new FawePaster(logger);
            int blocksPlaced = paster.pasteWorld(worldDir, world, snappedMinX, snappedMinZ, metadata);
            logger.info("FAWE paste complete: " + blocksPlaced + " blocks placed");

            sendProgress("Terrain placed! " + blocksPlaced + " blocks");

            // Mark as live
            sector.setGeneratedAt(System.currentTimeMillis());
            sector.setGeneratedBy(requestedBy != null ? requestedBy.toString() : "SYSTEM");
            sector.setStatus(Sector.Status.LIVE);
            plugin.getSectorRegistry().register(sector);

            sendProgress("Sector '" + sector.getName() + "' is LIVE! Use /sector tp " + sector.getName());

            // Cleanup temp directory
            deleteRecursive(tempDir);

        } catch (Exception e) {
            logger.severe("Sector generation failed: " + e.getMessage());
            e.printStackTrace();
            sector.setStatus(Sector.Status.FAILED);
            sendProgress("FAILED: " + e.getMessage());
        }
    }

    private void sendProgress(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Component msg = Component.text("[WKit] " + message, NamedTextColor.GREEN);
            if (requestedBy != null) {
                Player player = Bukkit.getPlayer(requestedBy);
                if (player != null && player.isOnline()) {
                    player.sendMessage(msg);
                }
            }
            Bukkit.getConsoleSender().sendMessage(msg);
        });
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteRecursive(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
