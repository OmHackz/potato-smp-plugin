package dev.potatosmp.potato_stats.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player persisted counter block. One instance is kept in memory per known player and
 * flushed to {@code plugins/PotatoStats/data/<uuid>.json} so restarts do not lose data.
 */
public class PlayerData {
    public final UUID uuid;
    public String name;
    public long firstJoin;   // epoch millis
    public long lastSeen;    // epoch millis
    public long playtimeMinutes; // lifetime minutes played
    public String rank = "default";
    public final Map<String, Long> stats = new LinkedHashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        long now = System.currentTimeMillis();
        this.firstJoin = now;
        this.lastSeen = now;
    }
}
