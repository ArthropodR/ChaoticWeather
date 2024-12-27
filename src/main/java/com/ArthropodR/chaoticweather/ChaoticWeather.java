package com.ArthropodR.chaoticweather;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ChaoticWeather extends JavaPlugin implements Listener, TabCompleter, CommandExecutor {

    private final Random random = new Random();
    private WeatherEvents weatherEvents;
    private RandomWeatherEvents randomWeatherEvents;
    private TranslationManager translationManager;

    @Override
    public void onEnable() {
        // Ensure default config is saved if it doesn't exist
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Chaotic Weather plugin has been enabled!");

        // Initialize translation manager first
        translationManager = new TranslationManager(this);

        // Pass translationManager to other classes
        weatherEvents = new WeatherEvents(this, translationManager);
        randomWeatherEvents = new RandomWeatherEvents(this, translationManager);

        if (getConfig().getBoolean("random_events.enabled")) {
            randomWeatherEvents.startRandomEvents();
        }

        if (getConfig().getBoolean("events.rain_effects")) {
            weatherEvents.startRainEffectTask();
        }

        if (getConfig().getBoolean("events.plant_growth_enhancement")) {
            weatherEvents.startPlantGrowthTask();
        }

        // Register the command and tab completer
        getCommand("chaoticweather").setExecutor(this);
        getCommand("chaoticweather").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + translationManager.getMessage("language_usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("language")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + translationManager.getMessage("language_usage"));
                return true;
            }

            String languageCode = switch (args[1].toLowerCase()) {
                case "eng" -> "en";
                case "fr" -> "fr";
                case "es" -> "es";
                case "ru" -> "ru";
                default -> null;
            };

            if (languageCode == null) {
                sender.sendMessage(ChatColor.RED + translationManager.getMessage("invalid_language"));
                return true;
            }

            translationManager.loadLanguage(languageCode);
            sender.sendMessage(ChatColor.GREEN + translationManager.getMessage("language_change_success"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("command_only_for_players"));
            return true;
        }

        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("chaoticweather.reload")) {
                player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission_reload"));
                return true;
            }

            reloadConfig();
            player.sendMessage(ChatColor.GREEN + translationManager.getMessage("config_reloaded"));
            return true;
        } else if (args[0].equalsIgnoreCase("summon")) {
            if (!player.hasPermission("chaoticweather.summon")) {
                player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission_summon"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + translationManager.getMessage("usage_summon"));
                return true;
            }

            String eventName = args[1].toLowerCase();

            // Trigger event from RandomWeatherEvents
            randomWeatherEvents.summonEvent(player, eventName);
            return true;
        }

        sender.sendMessage(ChatColor.RED + translationManager.getMessage("unknown_command"));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "summon", "language");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            return Arrays.asList("meteor_shower", "meteor_impact", "treasure_meteor", "hurricane_winds", "hailstorm", "aurora_storm");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("language")) {
            return Arrays.asList("eng", "fr", "es", "ru");
        }
        return new ArrayList<>();
    }

    @Override
    public void onDisable() {
        getLogger().info("Chaotic Weather plugin has been disabled!");
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + translationManager.getMessage("storm_brewing"));
        }
    }
}
