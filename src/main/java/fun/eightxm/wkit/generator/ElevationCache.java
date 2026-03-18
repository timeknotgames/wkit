package fun.eightxm.wkit.generator;

import fun.eightxm.wkit.sector.GeoProjection;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Fetches and caches AWS Terrarium elevation tiles.
 * Mirrors Arnis's elevation_data.rs logic.
 *
 * Terrarium format: height = (R * 256 + G + B / 256) - 32768
 * Tile URL: https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png
 */
public class ElevationCache {

    private static final String TILE_URL_TEMPLATE =
        "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/%d/%d/%d.png";
    private static final int ZOOM = 12; // zoom 12 gives ~38m/pixel — good for 1:1 scale
    private static final int TILE_SIZE = 256;

    private final Path cacheDir;
    private final Logger logger;
    private final GeoProjection projection;

    // Cache decoded tile elevation grids in memory
    private final ConcurrentHashMap<String, float[][]> tileCache = new ConcurrentHashMap<>();

    public ElevationCache(Path cacheDir, GeoProjection projection, Logger logger) {
        this.cacheDir = cacheDir;
        this.projection = projection;
        this.logger = logger;
        try { Files.createDirectories(cacheDir); } catch (IOException ignored) {}
    }

    /**
     * Get elevation in meters at a given lat/lng.
     * Returns 0 if tile can't be fetched.
     */
    public float getElevation(double lat, double lng) {
        // Convert lat/lng to tile coordinates
        int tileX = lonToTileX(lng, ZOOM);
        int tileY = latToTileY(lat, ZOOM);
        String key = ZOOM + "/" + tileX + "/" + tileY;

        // Get or fetch tile
        float[][] grid = tileCache.computeIfAbsent(key, k -> loadTile(tileX, tileY));
        if (grid == null) return 0;

        // Get pixel position within tile
        double tileXfrac = lonToTileXFrac(lng, ZOOM) - tileX;
        double tileYfrac = latToTileYFrac(lat, ZOOM) - tileY;
        int px = Math.min((int)(tileXfrac * TILE_SIZE), TILE_SIZE - 1);
        int py = Math.min((int)(tileYfrac * TILE_SIZE), TILE_SIZE - 1);

        return grid[py][px];
    }

    /**
     * Get elevation at a Minecraft coordinate (relative to Mount Tahoma origin).
     */
    public float getElevationAtMC(int mcX, int mcZ) {
        double[] latLng = projection.toLatlng(mcX, mcZ);
        return getElevation(latLng[0], latLng[1]);
    }

    /**
     * Convert elevation in meters to Minecraft Y coordinate.
     * Uses a simple mapping: sea level (0m) = Y=-62, with 1 block per meter up to Y=319.
     * Compression applied if needed.
     */
    public int elevationToY(float elevationMeters, float minElev, float maxElev) {
        int groundLevel = -62;
        int maxY = 304; // leave buffer for structures
        int availableRange = maxY - groundLevel; // 366 blocks

        float elevRange = maxElev - minElev;
        if (elevRange <= 0) return groundLevel;

        float scale;
        if (elevRange <= availableRange) {
            scale = 1.0f; // 1:1 mapping
        } else {
            scale = availableRange / elevRange; // compress
        }

        float relative = (elevationMeters - minElev) * scale;
        return groundLevel + Math.round(relative);
    }

    private float[][] loadTile(int tileX, int tileY) {
        Path cached = cacheDir.resolve("z" + ZOOM + "_x" + tileX + "_y" + tileY + ".png");

        // Try cache first
        if (Files.exists(cached)) {
            return decodeTile(cached);
        }

        // Download
        String url = String.format(TILE_URL_TEMPLATE, ZOOM, tileX, tileY);
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "WKit/1.0");

            if (conn.getResponseCode() != 200) {
                logger.warning("Elevation tile fetch failed: " + url + " → " + conn.getResponseCode());
                return null;
            }

            try (InputStream is = conn.getInputStream()) {
                Files.copy(is, cached, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Cached elevation tile: " + cached.getFileName());
            return decodeTile(cached);

        } catch (Exception e) {
            logger.warning("Failed to fetch elevation tile: " + e.getMessage());
            return null;
        }
    }

    private float[][] decodeTile(Path pngFile) {
        try {
            BufferedImage img = ImageIO.read(pngFile.toFile());
            if (img == null) return null;

            float[][] grid = new float[TILE_SIZE][TILE_SIZE];
            for (int y = 0; y < Math.min(img.getHeight(), TILE_SIZE); y++) {
                for (int x = 0; x < Math.min(img.getWidth(), TILE_SIZE); x++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // Terrarium encoding
                    grid[y][x] = (r * 256.0f + g + b / 256.0f) - 32768.0f;
                }
            }
            return grid;
        } catch (Exception e) {
            logger.warning("Failed to decode elevation tile: " + e.getMessage());
            return null;
        }
    }

    // Slippy map tile math
    private static int lonToTileX(double lng, int zoom) {
        return (int) Math.floor(lonToTileXFrac(lng, zoom));
    }
    private static int latToTileY(double lat, int zoom) {
        return (int) Math.floor(latToTileYFrac(lat, zoom));
    }
    private static double lonToTileXFrac(double lng, int zoom) {
        return (lng + 180.0) / 360.0 * (1 << zoom);
    }
    private static double latToTileYFrac(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * (1 << zoom);
    }
}
