package com.ArthropodR.chaoticweather;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
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

import java.util.*;

public class ChaoticWeather extends JavaPlugin implements Listener, TabCompleter, CommandExecutor {

    private final Random random = new Random();
    private WeatherEvents weatherEvents;
    private RandomWeatherEvents randomWeatherEvents;
    private TranslationManager translationManager;
    private RestrictedRegionsManager restrictedRegionsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        translationManager = new TranslationManager(this);
        restrictedRegionsManager = new RestrictedRegionsManager(this);
        weatherEvents = new WeatherEvents(this, translationManager, restrictedRegionsManager);
        randomWeatherEvents = new RandomWeatherEvents(this, translationManager, restrictedRegionsManager);

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Chaotic Weather plugin has been enabled!");

        // Start scheduled tasks if enabled in config
        if (getConfig().getBoolean("random_events.enabled")) {
            randomWeatherEvents.startRandomEvents();
        }

        if (getConfig().getBoolean("events.rain_effects")) {
            weatherEvents.startRainEffectTask();
        }

        if (getConfig().getBoolean("events.plant_growth_enhancement")) {
            weatherEvents.startPlantGrowthTask();
        }

        if (getConfig().getBoolean("events.thunderstorm_effects")) {
            weatherEvents.startThunderstormEffects();
        }

        // Register commands
        Objects.requireNonNull(getCommand("chaoticweather")).setExecutor(this);
        Objects.requireNonNull(getCommand("chaoticweather")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + translationManager.getMessage("language_usage"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + translationManager.getMessage("command_only_for_players"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "restrict" -> handleRestrictCommand(player, args);
            case "reload" -> handleReloadCommand(player);
            case "summon" -> handleSummonCommand(player, args);
            case "language" -> handleLanguageCommand(player, args);
            default -> sender.sendMessage(ChatColor.RED + translationManager.getMessage("unknown_command"));
        }
        return true;
    }

    private void handleRestrictCommand(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /chaoticweather restrict <event_name>");
            return;
        }

        String eventName = args[1].toLowerCase();
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

        if (worldEdit == null) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("worldedit_missing"));
            return;
        }

        try {
            // Get the WorldEdit selection
            Region selection = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));

            if (selection == null) {
                player.sendMessage(ChatColor.RED + translationManager.getMessage("worldedit_no_selection"));
                return;
            }

            // Convert BlockVector3 to Location using BukkitAdapter
            World world = player.getWorld();
            BlockVector3 minPoint = selection.getMinimumPoint();
            BlockVector3 maxPoint = selection.getMaximumPoint();

            Location pos1 = new Location(world, minPoint.getX(), minPoint.getY(), minPoint.getZ());
            Location pos2 = new Location(world, maxPoint.getX(), maxPoint.getY(), maxPoint.getZ());

            // Add the restricted region using the converted Locations
            restrictedRegionsManager.addRestrictedRegion(eventName, pos1, pos2);
            player.sendMessage(ChatColor.GREEN + translationManager.getMessage("event_restricted").replace("%event%", eventName));
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("error_restricting"));
            e.printStackTrace(); // Added for better error logging
        }
    }

    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("chaoticweather.reload")) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission_reload"));
            return;
        }

        reloadConfig();
        translationManager.reloadMessages();
        restrictedRegionsManager.loadRestrictedRegions();
        player.sendMessage(ChatColor.GREEN + translationManager.getMessage("config_reloaded"));
    }

    private void handleSummonCommand(Player player, String[] args) {
        if (!player.hasPermission("chaoticweather.summon")) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission_summon"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + translationManager.getMessage("usage_summon"));
            return;
        }

        String eventName = args[1].toLowerCase();
        if (restrictedRegionsManager.isRestricted(eventName, player.getLocation())) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("event_restricted_location"));
            return;
        }

        randomWeatherEvents.summonEvent(player, eventName);
    }

    private void handleLanguageCommand(Player player, String[] args) {
        if (!player.hasPermission("chaoticweather.language")) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission_language"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + translationManager.getMessage("language_usage"));
            return;
        }

        String languageCode = args[1].toLowerCase();
        if (translationManager.setLanguage(languageCode)) {
            player.sendMessage(ChatColor.GREEN + translationManager.getMessage("language_change_success"));
        } else {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("invalid_language"));
        }
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> commands = Arrays.asList("reload", "summon", "language", "restrict");

            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
            return completions;
        } else if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            switch (args[0].toLowerCase()) {
                case "summon", "restrict" -> {
                    List<String> events = Arrays.asList("meteor_shower", "meteor_impact", "treasure_meteor",
                            "hurricane_winds", "hailstorm", "aurora_storm", "rain_effects", "thunderstorm_effects");
                    for (String event : events) {
                        if (event.startsWith(args[1].toLowerCase())) {
                            completions.add(event);
                        }
                    }
                }
                case "language" -> {
                    List<String> languages = Arrays.asList("en", "fr", "es", "ru");
                    for (String lang : languages) {
                        if (lang.startsWith(args[1].toLowerCase())) {
                            completions.add(lang);
                        }
                    }
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }

    @Override
    public void onDisable() {
        // Cancel all tasks and clean up
        Bukkit.getScheduler().cancelTasks(this);
        weatherEvents = null;
        randomWeatherEvents = null;
        translationManager = null;
        restrictedRegionsManager = null;

        getLogger().info("Chaotic Weather plugin has been disabled!");
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + translationManager.getMessage("storm_brewing"));
        }
    }
}
