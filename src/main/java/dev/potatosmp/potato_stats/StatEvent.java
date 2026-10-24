package dev.potatosmp.potato_stats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single stat event produced by a listener before it is coalesced by {@link EventBuffer}
 * and handed to {@link dev.potatosmp.potato_stats.net.StatsPusher} for delivery.
 */
public class StatEvent {
    public final String type;          // player_join, player_leave, block_mined, ...
    public final String player;        // player name
    public final String uuid;          // player/mob UUID
    public final long at;              // epoch millis
    public final Map<String, Object> payload; // extra fields (stat, value, mob, victim, killer, ...)
    public final boolean coalesce;     // true if this type should be merged per player between flushes
    public final int count;            // delta to add when coalescing (1 for most events)

    public StatEvent(String type, String player, String uuid, long at,
                     Map<String, Object> payload, boolean coalesce, int count) {
        this.type = type;
        this.player = player;
        this.uuid = uuid;
        this.at = at;
        this.payload = payload != null ? payload : new LinkedHashMap<>();
        this.coalesce = coalesce;
        this.count = count;
    }

    public StatEvent(String type, String player, String uuid, long at) {
        this(type, player, uuid, at, new LinkedHashMap<>(), false, 1);
    }
}
