package dev.potatosmp.potato_stats;

import dev.potatosmp.potato_stats.config.ConfigManager;
import dev.potatosmp.potato_stats.data.DataStore;
import dev.potatosmp.potato_stats.http.HttpApi;
import dev.potatosmp.potato_stats.listeners.BlockListener;
import dev.potatosmp.potato_stats.listeners.DeathListener;
import dev.potatosmp.potato_stats.listeners.JoinListener;
import dev.potatosmp.potato_stats.listeners.LeaveListener;
import dev.potatosmp.potato_stats.listeners.MobListener;
import dev.potatosmp.potato_stats.listeners.StatListener;
import dev.potatosmp.potato_stats.listeners.KillListener;
import dev.potatosmp.potato_stats.EventBuffer;
import dev.potatosmp.potato_stats.net.StatsPusher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for the PotatoStats Paper plugin.
 * Collects server/player events and pushes them to the PotatoSMP website REST API.
 */
public class PotatoStatsPlugin extends JavaPlugin {
    private static PotatoStatsPlugin instance;

    private ConfigManager config;
    private DataStore dataStore;
    private EventBuffer buffer;
    private StatsPusher pusher;
    private HttpApi httpApi;
    private BukkitTask pushTask;

    /** Join timestamps used to compute per-session playtime on quit. */
    public final Map<String, Long> joinTimes = new ConcurrentHashMap<>();

    public static PotatoStatsPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        config = new ConfigManager(this);

        String dataDir = config.getString("data.dir", "plugins/PotatoStats/data");
        dataStore = new DataStore(getLogger(), dataDir);
        dataStore.load();

        buffer = new EventBuffer();

        String serverId = config.getString("server-id", "potatosmp");
        String url = config.getString("push.url", "https://potato-smp.pages.dev/api/collect");
        String secret = config.getString("push.secret", "change-me");
        int interval = config.getInt("push.interval-seconds", 30);
        int flushMax = config.getInt("push.flush-max-events", 1000);

        pusher = new StatsPusher(this, buffer, serverId, url, secret, interval, flushMax);

        // Register all event listeners.
        new JoinListener(this, buffer, dataStore, joinTimes).register(this);
        new LeaveListener(this, buffer, dataStore, joinTimes).register(this);
        new BlockListener(this, buffer, dataStore).register(this);
        new KillListener(this, buffer, dataStore).register(this);
        new DeathListener(this, buffer, dataStore).register(this);
        new MobListener(this, buffer, dataStore).register(this);
        new StatListener(this, buffer, dataStore).register(this);

        pushTask = pusher.start();

        // Persist per-player counters to disk every 5 minutes.
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, dataStore::flush, 5L * 60 * 20, 5L * 60 * 20);

        // Optional read-only HTTP API for the website (disabled by default).
        if (config.getBoolean("http.enabled", false)) {
            int port = config.getInt("http.port", 8790);
            httpApi = new HttpApi(this, dataStore, port);
            httpApi.start();
        }

        getLogger().info("PotatoStats enabled (Paper 26.3, api-version 26.3)");
    }

    @Override
    public void onDisable() {
        if (pushTask != null) pushTask.cancel();
        if (httpApi != null) httpApi.stop();
        if (dataStore != null) dataStore.flush();
        instance = null;
        getLogger().info("PotatoStats disabled");
    }

    public ConfigManager config() {
        return config;
    }

    public DataStore dataStore() {
        return dataStore;
    }

    public EventBuffer buffer() {
        return buffer;
    }
}
