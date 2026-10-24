package dev.potatosmp.potato_stats.net;

import dev.potatosmp.potato_stats.Json;
import dev.potatosmp.potato_stats.PotatoStatsPlugin;
import dev.potatosmp.potato_stats.EventBuffer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Pushes coalesced stat batches to the website REST API off the main thread.
 *
 * <p>POSTs {@code {serverId, sentAt, events[]}} to the configured URL with an
 * {@code Authorization: Bearer <secret>} header on a fixed async schedule.
 *
 * <p>On connection failure it backs off (30s, 60s, 120s, capped at 5 min) and re-queues the
 * failed events so they are retried. It never blocks the server main thread and never crashes
 * it: all HTTP is asynchronous via {@link HttpClient#sendAsync}.
 */
public class StatsPusher {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final String serverId;
    private final String url;
    private final String secret;
    private final int intervalTicks;
    private final int flushMaxEvents;
    private final HttpClient httpClient;

    private volatile long nextRetryAt = 0;
    private volatile int backoffLevel = 0;

    public StatsPusher(PotatoStatsPlugin plugin, EventBuffer buffer, String serverId,
                       String url, String secret, int intervalSeconds, int flushMaxEvents) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.serverId = serverId;
        this.url = url;
        this.secret = secret;
        this.intervalTicks = intervalSeconds * 20;
        this.flushMaxEvents = flushMaxEvents;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public BukkitTask start() {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flush, 0L, intervalTicks);
    }

    public void stop() {
        // Scheduled tasks are cancelled by JavaPlugin when the plugin disables.
    }

    private void flush() {
        long now = System.currentTimeMillis();
        if (now < nextRetryAt) {
            return; // still backing off
        }

        List<EventBuffer.PendingEntry> entries = buffer.drainUpTo(flushMaxEvents);
        if (entries.isEmpty()) {
            return;
        }

        List<Map<String, Object>> events = new ArrayList<>(entries.size());
        for (EventBuffer.PendingEntry e : entries) {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("type", e.type);
            ev.put("player", e.player);
            ev.put("uuid", e.uuid);
            ev.put("at", formatAt(e.at));
            if (e.count > 1) {
                ev.put("value", e.count);
            }
            ev.putAll(e.payload);
            events.add(ev);
        }

        Map<String, Object> batch = new LinkedHashMap<>();
        batch.put("serverId", serverId);
        batch.put("sentAt", formatAt(now));
        batch.put("events", events);

        String body = Json.serialize(batch);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + secret)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    int code = resp.statusCode();
                    if (code >= 200 && code < 300) {
                        onSuccess();
                        plugin.getLogger().fine("PotatoStats: pushed " + events.size()
                                + " stat events (HTTP " + code + ")");
                    } else {
                        plugin.getLogger().warning("PotatoStats: push rejected with HTTP "
                                + code + "; scheduling retry");
                        onFailure(entries);
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("PotatoStats: push failed: " + ex.getMessage()
                            + "; scheduling retry");
                    onFailure(entries);
                    return null;
                });
    }

    private void onSuccess() {
        nextRetryAt = 0;
        backoffLevel = 0;
    }

    private void onFailure(List<EventBuffer.PendingEntry> entries) {
        // Re-queue so they are retried once backoff elapses.
        for (EventBuffer.PendingEntry e : entries) {
            buffer.reOffer(e);
        }
        nextRetryAt = System.currentTimeMillis() + backoffDelayMs();
        backoffLevel++;
    }

    private long backoffDelayMs() {
        if (backoffLevel == 0) return 30_000;
        if (backoffLevel == 1) return 60_000;
        if (backoffLevel == 2) return 120_000;
        return 300_000; // cap at 5 min
    }

    private static String formatAt(long millis) {
        return Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .replace("+00:00", "Z");
    }
}
