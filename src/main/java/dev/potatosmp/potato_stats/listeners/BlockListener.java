package dev.potatosmp.potato_stats.listeners;

import dev.potatosmp.potato_stats.data.DataStore;
import dev.potatosmp.potato_stats.EventBuffer;
import dev.potatosmp.potato_stats.PotatoStatsPlugin;
import dev.potatosmp.potato_stats.StatEvent;
import dev.potatosmp.potato_stats.data.PlayerData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * block_mined / block_placed: coalesced per-player counts between flushes.
 */
public class BlockListener implements Listener {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final DataStore dataStore;

    public BlockListener(PotatoStatsPlugin plugin, EventBuffer buffer, DataStore dataStore) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.dataStore = dataStore;
    }

    public void register(PotatoStatsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        record(event.getPlayer(), event.getBlock(), "block_mined");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        record(event.getPlayer(), event.getBlock(), "block_placed");
    }

    private void record(Player player, Block block, String type) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        long at = System.currentTimeMillis();

        dataStore.touch(uuid, name);
        dataStore.increment(uuid, type, 1);

        buffer.offer(new StatEvent(type, name, uuid.toString(), at, null, true, 1));
    }
}
