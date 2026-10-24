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
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * death: one event per player death.
 */
public class DeathListener implements Listener {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final DataStore dataStore;

    public DeathListener(PotatoStatsPlugin plugin, EventBuffer buffer, DataStore dataStore) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.dataStore = dataStore;
    }

    public void register(PotatoStatsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        long at = System.currentTimeMillis();

        dataStore.touch(uuid, name);
        dataStore.increment(uuid, "deaths", 1);

        buffer.offer(new StatEvent("death", name, uuid.toString(), at, null, false, 1));

        plugin.getLogger().fine("PotatoStats: death " + name);
    }
}
