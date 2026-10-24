package dev.potatosmp.potato_stats.data;

import dev.potatosmp.potato_stats.Json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.logging.Logger;

/**
 * Loads and persists {@link PlayerData} to JSON files under the data directory.
 *
 * <p>Flushed on {@link org.bukkit.event.player.PlayerQuitEvent} and on a 5-minute timer so
 * per-player counters survive restarts.
 */
public class DataStore {
    private final Logger logger;
    private final Path dir;
    private final Map<UUID, PlayerData> players = new LinkedHashMap<>();

    public DataStore(Logger logger, String dir) {
        this.logger = logger;
        this.dir = Paths.get(dir);
    }

    public void load() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.warning("PotatoStats: failed to create data dir '" + dir + "': " + e.getMessage());
            return;
        }
        int count = 0;
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> jsonFiles = files.filter(p -> p.toString().endsWith(".json")).toList();
            count = jsonFiles.size();
            jsonFiles.forEach(this::loadFile);
        } catch (IOException e) {
            logger.warning("PotatoStats: failed to list data dir '" + dir + "': " + e.getMessage());
        }
        logger.info("PotatoStats: loaded player data for " + players.size() + " players (" + count + " files)");
    }

    private void loadFile(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            Object parsed = Json.parse(sb.toString());
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) parsed;
            UUID uuid = UUID.fromString(String.valueOf(map.get("uuid")));
            PlayerData data = new PlayerData(uuid);
            data.name = String.valueOf(map.getOrDefault("name", data.name));
            data.firstJoin = asLong(map.get("firstJoin"));
            data.lastSeen = asLong(map.get("lastSeen"));
            data.playtimeMinutes = asLong(map.get("playtimeMinutes"));
            data.rank = String.valueOf(map.getOrDefault("rank", "default"));
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) map.getOrDefault("stats", Map.of());
            for (Map.Entry<String, Object> e : stats.entrySet()) {
                data.stats.put(e.getKey(), asLong(e.getValue()));
            }
            players.put(uuid, data);
        } catch (Exception e) {
            logger.warning("PotatoStats: failed to load player data from " + file.getFileName() + ": " + e.getMessage());
        }
    }

    public synchronized PlayerData getOrCreate(UUID uuid, String name) {
        PlayerData data = players.get(uuid);
        if (data == null) {
            data = new PlayerData(uuid);
            players.put(uuid, data);
        }
        if (name != null && !name.equals(data.name)) {
            data.name = name;
        }
        return data;
    }

    public synchronized PlayerData get(UUID uuid) {
        return players.get(uuid);
    }

    public synchronized Map<UUID, PlayerData> getAll() {
        return new LinkedHashMap<>(players);
    }

    public synchronized void touch(UUID uuid, String name) {
        PlayerData data = getOrCreate(uuid, name);
        data.lastSeen = System.currentTimeMillis();
    }

    public synchronized void increment(UUID uuid, String stat, long amount) {
        PlayerData data = players.get(uuid);
        if (data == null) return;
        data.stats.merge(stat, amount, Long::sum);
    }

    public synchronized void addPlaytime(UUID uuid, long minutes) {
        if (minutes <= 0) return;
        PlayerData data = players.get(uuid);
        if (data == null) return;
        data.playtimeMinutes += minutes;
    }

    /** Writes every known player to disk. */
    public synchronized void flush() {
        for (PlayerData data : players.values()) {
            save(data);
        }
    }

    private void save(PlayerData data) {
        try {
            Files.createDirectories(dir);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("uuid", data.uuid.toString());
            map.put("name", data.name);
            map.put("firstJoin", data.firstJoin);
            map.put("lastSeen", data.lastSeen);
            map.put("playtimeMinutes", data.playtimeMinutes);
            map.put("rank", data.rank);
            map.put("stats", new LinkedHashMap<>(data.stats));
            Path file = dir.resolve(data.uuid.toString() + ".json");
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(Json.serialize(map));
            }
        } catch (IOException e) {
            logger.warning("PotatoStats: failed to save player data for " + data.uuid + ": " + e.getMessage());
        }
    }

    private static long asLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
