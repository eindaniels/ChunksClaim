package de.eindaniel.chunksClaim.i18n;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Internationalization {
    private final FileConfiguration defaultLangConfig;
    private final JavaPlugin plugin;

    public Internationalization(JavaPlugin plugin) {
        this.plugin = plugin;
        defaultLangConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "run/plugins/ChunksClaim/lang/DE.yml"));
    }

    public FileConfiguration getLanguageConfig(String language) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (langFile.exists()) {
            return YamlConfiguration.loadConfiguration(langFile);
        }
        return defaultLangConfig;
    }
}