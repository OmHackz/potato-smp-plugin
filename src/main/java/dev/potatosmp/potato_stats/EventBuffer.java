package dev.potatosmp.potato_stats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coalescing buffer for pending stat events.
 *
 * <p>{@code block_mined}, {@code block_placed} and {@code stat} events are coalesced per player
 * (per player+stat for {@code stat}) so a player spamming blocks does not blow up the buffer.
 * All other events are stored individually.
 *
 * <p>The buffer is capped at 10,000 entries; when it overflows the oldest entries are dropped
 * and a warning is logged. All access is synchronized.
 */
public class EventBuffer {
    private static final int MAX_ENTRIES = 10_000;

    private final LinkedHashMap<String, PendingEntry> entries = new LinkedHashMap<>();
    private long seq = 0;

    public synchronized void offer(StatEvent event) {
        addEntry(new PendingEntry(event));
    }

    public synchronized void reOffer(PendingEntry entry) {
        addEntry(entry);
    }

    /**
     * Removes and returns up to {@code max} pending entries (oldest first), leaving any
     * remaining entries in the buffer for the next flush.
     */
    public synchronized List<PendingEntry> drainUpTo(int max) {
        List<PendingEntry> out = new ArrayList<>(Math.min(max, entries.size()));
        Iterator<Map.Entry<String, PendingEntry>> it = entries.entrySet().iterator();
        while (it.hasNext() && out.size() < max) {
            out.add(it.next().getValue());
            it.remove();
        }
        return out;
    }

    public synchronized int size() {
        return entries.size();
    }

    private synchronized void addEntry(PendingEntry entry) {
        String key;
        if (entry.coalesce) {
            key = entry.type + ":" + entry.uuid + ":" + entry.payload.getOrDefault("stat", "");
        } else {
            key = entry.type + ":" + entry.uuid + ":" + (seq++);
        }
        PendingEntry existing = entries.get(key);
        if (existing != null) {
            existing.count += entry.count;
            existing.at = Math.max(existing.at, entry.at);
            existing.payload.putAll(entry.payload);
        } else {
            entries.put(key, entry);
        }
        cap();
    }

    private void cap() {
        while (entries.size() > MAX_ENTRIES) {
            Iterator<Map.Entry<String, PendingEntry>> it = entries.entrySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    public static final class PendingEntry {
        public final String type;
        public final String player;
        public final String uuid;
        public long at;
        public int count;
        public final Map<String, Object> payload;
        public final boolean coalesce;

        public PendingEntry(StatEvent event) {
            this.type = event.type;
            this.player = event.player;
            this.uuid = event.uuid;
            this.at = event.at;
            this.count = event.count;
            this.payload = new LinkedHashMap<>(event.payload);
            this.coalesce = event.coalesce;
        }
    }
}
