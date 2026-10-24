package dev.potatosmp.potato_stats.listeners;

import dev.potatosmp.potato_stats.data.DataStore;
import dev.potatosmp.potato_stats.EventBuffer;
import dev.potatosmp.potato_stats.PotatoStatsPlugin;
import dev.potatosmp.potato_stats.StatEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

/**
 * stat: Paper's canonical per-player vanilla statistic feed (playtime, distance walked,
 * mob kills, items mined/placed, etc.). Coalesced per player per statistic between flushes.
 */
public class StatListener implements Listener {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final DataStore dataStore;

    public StatListener(PotatoStatsPlugin plugin, EventBuffer buffer, DataStore dataStore) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.dataStore = dataStore;
    }

    public void register(PotatoStatsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onStat(PlayerStatisticIncrementEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String stat = event.getStatistic().name();
        int value = Math.max(1, event.getNewValue() - event.getPreviousValue());
        long at = System.currentTimeMillis();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stat", stat);
        payload.put("value", value);

        dataStore.touch(uuid, name);
        dataStore.increment(uuid, stat, value);

        buffer.offer(new StatEvent("stat", name, uuid.toString(), at, payload, true, value));

        plugin.getLogger().fine("PotatoStats: stat " + name + " " + stat + " +=" + value);
    }
}
