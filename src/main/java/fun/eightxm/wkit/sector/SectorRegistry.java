package fun.eightxm.wkit.sector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SectorRegistry {

    private final File dataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Sector> sectors = new ConcurrentHashMap<>();

    public SectorRegistry(File dataFile) {
        this.dataFile = dataFile;
    }

    public void load() {
        if (!dataFile.exists()) {
            return;
        }
        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<List<Sector>>(){}.getType();
            List<Sector> list = gson.fromJson(reader, type);
            if (list != null) {
                for (Sector s : list) {
                    sectors.put(s.getId(), s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        dataFile.getParentFile().mkdirs();
        // Atomic write: write to temp file, then rename to prevent corruption on crash
        File tmpFile = new File(dataFile.getParentFile(), dataFile.getName() + ".tmp");
        try (Writer writer = new FileWriter(tmpFile)) {
            gson.toJson(new ArrayList<>(sectors.values()), writer);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            Files.move(tmpFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // ATOMIC_MOVE may not be supported on all filesystems — fall back to plain replace
            try {
                Files.move(tmpFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void register(Sector sector) {
        sectors.put(sector.getId(), sector);
        save();
    }

    public Sector findById(String id) {
        return sectors.get(id);
    }

    public Sector findByName(String name) {
        return sectors.values().stream()
            .filter(s -> s.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public Sector findByNameOrId(String query) {
        Sector s = findById(query);
        return s != null ? s : findByName(query);
    }

    public Sector findByCoords(int mcX, int mcZ) {
        return sectors.values().stream()
            .filter(s -> s.containsMC(mcX, mcZ))
            .findFirst()
            .orElse(null);
    }

    public Collection<Sector> getAll() {
        return Collections.unmodifiableCollection(sectors.values());
    }

    /**
     * Check if a proposed MC bounding box overlaps any existing LIVE sector.
     */
    public boolean checkOverlap(int minX, int minZ, int maxX, int maxZ) {
        return sectors.values().stream()
            .filter(s -> s.getStatus() == Sector.Status.LIVE)
            .anyMatch(s ->
                s.getMcMinX() <= maxX && s.getMcMaxX() >= minX &&
                s.getMcMinZ() <= maxZ && s.getMcMaxZ() >= minZ
            );
    }

    public void remove(String id) {
        sectors.remove(id);
        save();
    }
}
