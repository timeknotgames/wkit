package fun.eightxm.wkit.arnis;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.mca.Chunk;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;

import org.bukkit.World;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Uses FAWE to paste Arnis-generated terrain by reading raw NBT data
 * directly from the "Level" wrapper format that Arnis produces.
 */
public class FawePaster {

    private final Logger logger;

    public FawePaster(Logger logger) {
        this.logger = logger;
    }

    public int pasteWorld(Path arnisWorldDir, World targetWorld, int offsetX, int offsetZ,
                          ArnisMetadata metadata) throws Exception {

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(targetWorld);
        Path regionDir = arnisWorldDir.resolve("region");

        if (!Files.isDirectory(regionDir)) {
            throw new IOException("No region directory in Arnis output: " + arnisWorldDir);
        }

        int totalBlocks = 0;

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(weWorld)
                .maxBlocks(-1)
                .build()) {

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(regionDir, "r.*.*.mca")) {
                for (Path file : stream) {
                    try {
                        MCAFile mca = MCAUtil.read(file.toFile());
                        logger.info("Reading region file: " + file.getFileName());

                        for (int cx = 0; cx < 32; cx++) {
                            for (int cz = 0; cz < 32; cz++) {
                                Chunk chunk = mca.getChunk(cx, cz);
                                if (chunk == null) continue;

                                CompoundTag handle = chunk.getHandle();
                                if (handle == null) continue;

                                // Arnis wraps data in "Level" compound
                                CompoundTag level = handle.containsKey("Level")
                                    ? handle.getCompoundTag("Level")
                                    : handle;

                                int chunkX = level.getInt("xPos");
                                int chunkZ = level.getInt("zPos");

                                // Get sections from raw NBT (lowercase 's')
                                ListTag<?> sections = null;
                                if (level.containsKey("sections")) {
                                    sections = level.getListTag("sections");
                                } else if (level.containsKey("Sections")) {
                                    sections = level.getListTag("Sections");
                                }

                                if (sections == null || sections.size() == 0) continue;

                                totalBlocks += pasteSections(editSession, sections,
                                    chunkX, chunkZ, offsetX, offsetZ);
                            }
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to read " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

            logger.info("FAWE EditSession: placed " + totalBlocks + " blocks, flushing...");
        }

        return totalBlocks;
    }

    private int pasteSections(EditSession session, ListTag<?> sections,
                               int chunkX, int chunkZ, int offsetX, int offsetZ) {
        int placed = 0;

        for (int i = 0; i < sections.size(); i++) {
            CompoundTag sec = (CompoundTag) sections.get(i);
            int sectionY = sec.getByte("Y");

            if (!sec.containsKey("block_states")) continue;
            CompoundTag blockStates = sec.getCompoundTag("block_states");

            if (!blockStates.containsKey("palette")) continue;
            ListTag<?> palette = blockStates.getListTag("palette");

            if (palette.size() == 0) continue;

            // Build palette array
            BlockState[] paletteBlocks = new BlockState[palette.size()];
            boolean hasNonAir = false;
            for (int p = 0; p < palette.size(); p++) {
                CompoundTag entry = (CompoundTag) palette.get(p);
                String name = entry.getString("Name");
                if (name == null || name.isEmpty()) name = "minecraft:air";

                BlockType type = BlockTypes.get(name);
                if (type == null) {
                    paletteBlocks[p] = BlockTypes.AIR.getDefaultState();
                } else {
                    paletteBlocks[p] = type.getDefaultState();
                    if (!name.equals("minecraft:air")) hasNonAir = true;

                    // Apply properties
                    if (entry.containsKey("Properties")) {
                        CompoundTag props = entry.getCompoundTag("Properties");
                        for (String key : props.keySet()) {
                            try {
                                paletteBlocks[p] = paletteBlocks[p].with(
                                    type.getProperty(key), props.getString(key));
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            if (!hasNonAir) continue;

            // Decode block data
            if (palette.size() == 1) {
                // Uniform section — all blocks are palette[0]
                if (paletteBlocks[0].getBlockType() == BlockTypes.AIR) continue;

                int baseY = sectionY * 16;
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            int wx = chunkX * 16 + x + offsetX;
                            int wy = baseY + y;
                            int wz = chunkZ * 16 + z + offsetZ;
                            try {
                                session.setBlock(wx, wy, wz, paletteBlocks[0]);
                                placed++;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } else if (blockStates.containsKey("data")) {
                // Packed long array — decode indices
                long[] data = blockStates.getLongArrayTag("data").getValue();
                int bitsPerEntry = Math.max(4, (int) Math.ceil(Math.log(palette.size()) / Math.log(2)));
                long mask = (1L << bitsPerEntry) - 1;
                int entriesPerLong = 64 / bitsPerEntry;

                int baseY = sectionY * 16;
                for (int blockIdx = 0; blockIdx < 4096; blockIdx++) {
                    int longIdx = blockIdx / entriesPerLong;
                    int bitOffset = (blockIdx % entriesPerLong) * bitsPerEntry;

                    if (longIdx >= data.length) break;

                    int paletteIdx = (int) ((data[longIdx] >> bitOffset) & mask);
                    if (paletteIdx >= paletteBlocks.length) continue;

                    BlockState block = paletteBlocks[paletteIdx];
                    if (block.getBlockType() == BlockTypes.AIR) continue;

                    // Decode XYZ from linear index (YZX order in MC)
                    int y = blockIdx >> 8;         // blockIdx / 256
                    int z = (blockIdx >> 4) & 0xF; // (blockIdx / 16) % 16
                    int x = blockIdx & 0xF;        // blockIdx % 16

                    int wx = chunkX * 16 + x + offsetX;
                    int wy = baseY + y;
                    int wz = chunkZ * 16 + z + offsetZ;

                    try {
                        session.setBlock(wx, wy, wz, block);
                        placed++;
                    } catch (Exception ignored) {}
                }
            }
        }

        return placed;
    }
}
