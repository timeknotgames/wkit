package fun.eightxm.wkit.region;

import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.tag.CompoundTag;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages relocation of Arnis-generated .mca region files into the live server world.
 * Arnis always generates at origin (0,0). This class copies region files to their
 * target position and patches chunk xPos/zPos NBT tags.
 */
public class RegionFileManager {

    private final Logger logger;

    public RegionFileManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Relocate all region files from a generated world to the target offset in the server world.
     *
     * @param sourceRegionDir  The region/ directory from Arnis output
     * @param targetRegionDir  The server world's region/ directory
     * @param offsetRegionX    Region offset in X (offsetBlocks / 512)
     * @param offsetRegionZ    Region offset in Z (offsetBlocks / 512)
     * @return List of relocated region file names
     */
    public List<String> relocateRegions(Path sourceRegionDir, Path targetRegionDir,
                                         int offsetRegionX, int offsetRegionZ) throws IOException {
        List<String> relocated = new ArrayList<>();

        if (!Files.isDirectory(sourceRegionDir)) {
            throw new IOException("Source region directory does not exist: " + sourceRegionDir);
        }

        Files.createDirectories(targetRegionDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceRegionDir, "r.*.*.mca")) {
            for (Path sourceFile : stream) {
                String fileName = sourceFile.getFileName().toString();
                int[] sourceCoords = parseRegionCoords(fileName);
                if (sourceCoords == null) {
                    logger.warning("Skipping unparseable region file: " + fileName);
                    continue;
                }

                int targetRx = sourceCoords[0] + offsetRegionX;
                int targetRz = sourceCoords[1] + offsetRegionZ;
                String targetName = RegionMath.regionFileName(targetRx, targetRz);
                Path targetFile = targetRegionDir.resolve(targetName);

                if (Files.exists(targetFile)) {
                    logger.warning("Target region file already exists, skipping: " + targetName);
                    continue;
                }

                // Copy file first
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

                // Patch chunk coordinates in the copied file
                patchChunkCoordinates(targetFile.toFile(), sourceCoords[0], sourceCoords[1],
                                       targetRx, targetRz);

                logger.info("Relocated " + fileName + " → " + targetName);
                relocated.add(targetName);
            }
        }

        return relocated;
    }

    /**
     * Patch xPos and zPos in every chunk of a region file to match its new position.
     */
    private void patchChunkCoordinates(File regionFile, int oldRegionX, int oldRegionZ,
                                        int newRegionX, int newRegionZ) throws IOException {
        MCAFile mca = MCAUtil.read(regionFile);

        for (int cx = 0; cx < 32; cx++) {
            for (int cz = 0; cz < 32; cz++) {
                Chunk chunk = mca.getChunk(cx, cz);
                if (chunk == null) continue;

                int newXPos = newRegionX * 32 + cx;
                int newZPos = newRegionZ * 32 + cz;

                // updateHandle writes the new xPos/zPos into the chunk's NBT
                chunk.updateHandle(newXPos, newZPos);
            }
        }

        MCAUtil.write(mca, regionFile);
    }

    /**
     * Parse region coordinates from a file name like "r.3.-5.mca".
     * Returns [regionX, regionZ] or null if unparseable.
     */
    static int[] parseRegionCoords(String fileName) {
        // r.X.Z.mca
        if (!fileName.startsWith("r.") || !fileName.endsWith(".mca")) return null;
        String inner = fileName.substring(2, fileName.length() - 4); // strip "r." and ".mca"
        // Split carefully to handle negative numbers
        // Format: "X.Z" where X and Z can be negative
        // Strategy: find the last '.' and split there
        int lastDot = inner.lastIndexOf('.');
        if (lastDot <= 0) return null;
        try {
            int x = Integer.parseInt(inner.substring(0, lastDot));
            int z = Integer.parseInt(inner.substring(lastDot + 1));
            return new int[]{x, z};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
