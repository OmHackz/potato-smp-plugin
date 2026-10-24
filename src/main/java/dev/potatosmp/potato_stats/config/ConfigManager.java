package dev.potatosmp.potato_stats.config;

import dev.potatosmp.potato_stats.PotatoStatsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Thin wrapper around {@link PotatoStatsPlugin#getConfig()} so the rest of the plugin does not
 * depend directly on JavaPlugin for configuration lookups.
 */
public class ConfigManager {
    private final PotatoStatsPlugin plugin;

    public ConfigManager(PotatoStatsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public FileConfiguration get() {
        return plugin.getConfig();
    }

    public String getString(String path) {
        return plugin.getConfig().getString(path);
    }

    public String getString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    public int getInt(String path) {
        return plugin.getConfig().getInt(path);
    }

    public int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }

    public long getLong(String path, long def) {
        return plugin.getConfig().getLong(path, def);
    }
}
