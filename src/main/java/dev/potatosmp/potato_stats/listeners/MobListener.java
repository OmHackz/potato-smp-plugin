package dev.potatosmp.potato_stats.listeners;

import dev.potatosmp.potato_stats.data.DataStore;
import dev.potatosmp.potato_stats.EventBuffer;
import dev.potatosmp.potato_stats.PotatoStatsPlugin;
import dev.potatosmp.potato_stats.StatEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * mob_kill: hostile/neutral mob deaths (tagged with mob type).
 * Player deaths are excluded (handled by {@link DeathListener}).
 */
public class MobListener implements Listener {
    private final PotatoStatsPlugin plugin;
    private final EventBuffer buffer;
    private final DataStore dataStore;

    // Hostile and neutral mobs worth tracking. Passive farm animals are excluded.
    private static final Set<EntityType> MOB_TYPES = Set.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
            EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.ENDERMAN,
            EntityType.WITHER_SKELETON, EntityType.WITCH, EntityType.BLAZE, EntityType.GHAST,
            EntityType.MAGMA_CUBE, EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED,
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.RAVAGER,
            EntityType.PHANTOM, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.SHULKER,
            EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.PIGLIN_BRUTE, EntityType.WARDEN,
            EntityType.VEX, EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.POLAR_BEAR,
            EntityType.BEE, EntityType.GOAT, EntityType.LLAMA
    );

    public MobListener(PotatoStatsPlugin plugin, EventBuffer buffer, DataStore dataStore) {
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
        if (entity instanceof Player) return; // player deaths handled by DeathListener

        EntityType type = entity.getType();
        if (!MOB_TYPES.contains(type)) return;

        UUID uuid = entity.getUniqueId();
        String name = entity.getCustomName() != null ? entity.getCustomName() : type.name();
        long at = System.currentTimeMillis();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mob", type.name());
        dataStore.increment(uuid, "mob_kills", 1);

        buffer.offer(new StatEvent("mob_kill", name, uuid.toString(), at, payload, false, 1));

        plugin.getLogger().fine("PotatoStats: mob_kill " + type.name());
    }
}
