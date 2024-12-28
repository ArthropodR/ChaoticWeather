package com.ArthropodR.chaoticweather;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.Arrays;

public class TranslationManager {
    private final JavaPlugin plugin;
    private final Map<String, String> messages;
    private String currentLanguage;
    private final String[] supportedLanguages = {"en", "es", "fr", "ru"};

    public TranslationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.currentLanguage = "en";

        // Create lang directory if it doesn't exist
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        saveDefaultLanguageFiles();
        loadLanguage(currentLanguage);
    }

    private void saveDefaultLanguageFiles() {
        for (String lang : supportedLanguages) {
            saveLanguageFile(lang);
        }
    }

    private void saveLanguageFile(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
    }

    public void loadLanguage(String languageCode) {
        messages.clear();
        plugin.getLogger().info("Loading language: " + languageCode);

        File langFile = new File(plugin.getDataFolder(), "lang/" + languageCode + ".yml");
        YamlConfiguration langConfig;

        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        } else {
            InputStream defaultStream = plugin.getResource("lang/" + languageCode + ".yml");
            if (defaultStream == null) {
                plugin.getLogger().warning("Language file not found: " + languageCode);
                if (!languageCode.equals("en")) {
                    loadLanguage("en");
                }
                return;
            }
            langConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
        }

        loadMessagesRecursively(langConfig, "messages");
        currentLanguage = languageCode;
        plugin.getLogger().info("Loaded " + messages.size() + " translations for " + languageCode);
    }

    private void loadMessagesRecursively(YamlConfiguration config, String path) {
        if (config.isConfigurationSection(path)) {
            for (String key : config.getConfigurationSection(path).getKeys(false)) {
                String newPath = path + "." + key;
                if (config.isConfigurationSection(newPath)) {
                    loadMessagesRecursively(config, newPath);
                } else {
                    messages.put(newPath, config.getString(newPath));
                }
            }
        } else {
            String value = config.getString(path);
            if (value != null) {
                messages.put(path, value);
            }
        }
    }

    public String getMessage(String key) {
        String fullKey = key.startsWith("messages.") ? key : "messages." + key;
        String message = messages.get(fullKey);

        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + fullKey);
            return "Missing translation key: " + fullKey;
        }
        return message;
    }

    public void reloadMessages() {
        loadLanguage(currentLanguage);
    }

    public boolean setLanguage(String languageCode) {
        if (Arrays.asList(supportedLanguages).contains(languageCode)) {
            loadLanguage(languageCode);
            return true;
        }
        return false;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}