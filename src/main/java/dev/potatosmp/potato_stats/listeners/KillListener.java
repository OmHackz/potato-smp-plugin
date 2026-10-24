package dev.potatosmp.potato_stats.listeners;

import dev.potatosmp.potato_stats.data.DataStore;
import dev.potatosmp.potato_stats.EventBuffer;
import dev.potatosmp.potato_stats.PotatoStatsPlugin;
import dev.potatosmp.potato_stats.StatEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * kill: player-vs-player kill.
 * Implemented via {@link EntityDeathEvent} where the victim is a player and the damage
 * cause is an {@link EntityDamageByEntityEvent} whose damager is a player.
 * (Paper 26.3 does not expose {@code PlayerKillEvent}.)
 * Payload: victim, killer (when the killer is a player).
 */
public class KillListener implements Listener {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final DataStore dataStore;

    public KillListener(PotatoStatsPlugin plugin, EventBuffer buffer, DataStore dataStore) {
        this.plugin = plugin;
        this.buffer = buffer;
        this.dataStore = dataStore;
    }

    public void register(PotatoStatsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) return; // only player victims

        Player victim = (Player) entity;
        UUID uuid = victim.getUniqueId();
        String victimName = victim.getName();
        long at = System.currentTimeMillis();

        dataStore.touch(uuid, victimName);

        EntityDamageEvent cause = entity.getLastDamageCause();
        Player killer = null;
        if (cause instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) cause).getDamager();
            if (damager instanceof Player) killer = (Player) damager;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("victim", victimName);
        if (killer != null) {
            payload.put("killer", killer.getName());
            dataStore.increment(killer.getUniqueId(), "kills", 1);
        }

        buffer.offer(new StatEvent("kill", victimName, uuid.toString(), at, payload, false, 1));

        plugin.getLogger().fine("PotatoStats: kill victim=" + victimName
                + " killer=" + (killer != null ? killer.getName() : "none"));
    }
}
