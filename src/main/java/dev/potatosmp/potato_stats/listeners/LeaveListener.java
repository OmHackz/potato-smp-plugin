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
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * player_leave: includes this-session playtime in minutes.
 */
public class LeaveListener implements Listener {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final DataStore dataStore;
    private final Map<String, Long> joinTimes;

    public LeaveListener(PotatoStatsPlugin plugin, EventBuffer buffer,
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
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        long at = System.currentTimeMillis();

        Long joinAt = joinTimes.remove(uuid.toString());
        long sessionMinutes = 0;
        if (joinAt != null) {
            sessionMinutes = Math.max(0, (at - joinAt) / 60_000);
        }

        dataStore.addPlaytime(uuid, sessionMinutes);
        dataStore.touch(uuid, name);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playtime_minutes", sessionMinutes);

        buffer.offer(new StatEvent("player_leave", name, uuid.toString(), at, payload, false, 1));

        plugin.getLogger().fine("PotatoStats: player_leave " + name + " (session=" + sessionMinutes + "m)");
    }
}
