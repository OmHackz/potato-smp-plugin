package dev.potatosmp.potato_stats.listeners;

import dev.potatosmp.potato_stats.data.DataStore;
import dev.potatosmp.potato_stats.EventBuffer;
import dev.potatosmp.potato_stats.PotatoStatsPlugin;
import dev.potatosmp.potato_stats.StatEvent;
import dev.potatosmp.potato_stats.data.PlayerData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * player_join: one event per join, with firstTime flag.
 */
public class JoinListener implements Listener {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final DataStore dataStore;
    private final Map<String, Long> joinTimes;

    public JoinListener(PotatoStatsPlugin plugin, EventBuffer buffer,
                        DataStore dataStore, Map<String, Long> joinTimes) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.dataStore = dataStore;
        this.joinTimes = joinTimes;
    }

    public void register(PotatoStatsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        long at = System.currentTimeMillis();

        boolean existed = dataStore.get(uuid) != null;
        PlayerData data = dataStore.getOrCreate(uuid, name);
        boolean firstTime = !existed;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firstTime", firstTime);

        buffer.offer(new StatEvent("player_join", name, uuid.toString(), at, payload, false, 1));
        joinTimes.put(uuid.toString(), at);
        dataStore.touch(uuid, name);

        plugin.getLogger().fine("PotatoStats: player_join " + name + " (firstTime=" + firstTime + ")");
    }
}
