package fun.eightxm.wkit.arnis;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Wraps execution of the Arnis CLI binary for sector generation.
 */
public class ArnisCLI {

    private final Path binaryPath;
    private final Logger logger;

    public ArnisCLI(Path binaryPath, Logger logger) {
        this.binaryPath = binaryPath;
        this.logger = logger;
    }

    /**
     * Run Arnis to generate a world from the given bbox.
     *
     * @param bbox         "minLat,minLng,maxLat,maxLng"
     * @param outputDir    Directory where Arnis will create the world
     * @param terrain      Enable terrain generation
     * @param scale        Blocks per meter
     * @param groundLevel  Base Y level
     * @param saveJsonFile Optional path to save OSM JSON
     * @param timeoutSecs  Generation timeout
     * @param progressCallback  Called with stdout lines for progress reporting
     * @return Path to the generated world directory (Arnis creates "Arnis World N" inside outputDir)
     */
    public Path generate(String bbox, Path outputDir, boolean terrain, double scale,
                         int groundLevel, Path saveJsonFile, int timeoutSecs,
                         Consumer<String> progressCallback) throws IOException, InterruptedException {

        Files.createDirectories(outputDir);

        List<String> cmd = new ArrayList<>();
        cmd.add(binaryPath.toString());
        cmd.add("--bbox=" + bbox);
        cmd.add("--output-dir=" + outputDir.toString());
        cmd.add("--scale=" + scale);
        cmd.add("--ground-level=" + groundLevel);

        if (terrain) {
            cmd.add("--terrain");
        }

        if (saveJsonFile != null) {
            cmd.add("--save-json-file=" + saveJsonFile.toString());
        }

        logger.info("Running Arnis: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Read stdout in a separate thread
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[Arnis] " + line);
                    if (progressCallback != null) {
                        progressCallback.accept(line);
                    }
                }
            } catch (IOException e) {
                logger.warning("Error reading Arnis output: " + e.getMessage());
            }
        });
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished = process.waitFor(timeoutSecs, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Arnis generation timed out after " + timeoutSecs + " seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Arnis exited with code " + exitCode);
        }

        // Find the generated world directory (Arnis creates "Arnis World N")
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir, "Arnis World*")) {
            for (Path worldDir : stream) {
                if (Files.isDirectory(worldDir) && Files.exists(worldDir.resolve("region"))) {
                    return worldDir;
                }
            }
        }

        throw new IOException("Arnis completed but no world directory found in " + outputDir);
    }

    /**
     * Check if the Arnis binary exists and is executable.
     */
    public boolean isAvailable() {
        return Files.isExecutable(binaryPath);
    }
}
