package com.ArthropodR.chaoticweather;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.*;
import org.bukkit.command.*;
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

    private List<String> disabledWorlds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        translationManager = new TranslationManager(this);
        restrictedRegionsManager = new RestrictedRegionsManager(this);
        weatherEvents = new WeatherEvents(this, translationManager, restrictedRegionsManager);
        randomWeatherEvents = new RandomWeatherEvents(this, translationManager, restrictedRegionsManager);

        disabledWorlds = getConfig().getStringList("disabled_worlds");

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info(translationManager.getMessage("plugin_enabled"));

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

        Objects.requireNonNull(getCommand("chaoticweather")).setExecutor(this);
        Objects.requireNonNull(getCommand("chaoticweather")).setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        weatherEvents = null;
        randomWeatherEvents = null;
        translationManager = null;
        restrictedRegionsManager = null;

        getLogger().info(translationManager.getMessage("plugin_disabled"));
    }

    private boolean isWorldDisabled(World world) {
        return disabledWorlds.contains(world.getName());
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
            case "world" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + translationManager.getMessage("usage_world"));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "enable" -> handleEnableWorldCommand(player, args);
                    case "disable" -> handleDisableWorldCommand(player, args);
                    default -> sender.sendMessage(ChatColor.RED + translationManager.getMessage("unknown_command"));
                }
            }
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
            player.sendMessage(ChatColor.YELLOW + translationManager.getMessage("usage_restrict"));
            return;
        }

        String eventName = args[1].toLowerCase();
        if (eventName.equals("weather_events")) {
            handleRestrictWeatherEventsCommand(player);
            return;
        }

        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

        if (worldEdit == null) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("worldedit_missing"));
            return;
        }

        try {
            Region selection = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));

            if (selection == null) {
                player.sendMessage(ChatColor.RED + translationManager.getMessage("worldedit_no_selection"));
                return;
            }

            BlockVector3 minPoint = selection.getMinimumPoint();
            BlockVector3 maxPoint = selection.getMaximumPoint();

            Location pos1 = new Location(player.getWorld(), minPoint.getX(), minPoint.getY(), minPoint.getZ());
            Location pos2 = new Location(player.getWorld(), maxPoint.getX(), maxPoint.getY(), maxPoint.getZ());

            restrictedRegionsManager.addRestrictedRegion(eventName, pos1, pos2);
            player.sendMessage(ChatColor.GREEN + translationManager.getMessage("event_restricted").replace("%event%", eventName));
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("error_restricting"));
            e.printStackTrace();
        }
    }

    private void handleRestrictWeatherEventsCommand(Player player) {
        WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

        if (worldEdit == null) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("worldedit_missing"));
            return;
        }

        try {
            Region selection = worldEdit.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));

            if (selection == null) {
                player.sendMessage(ChatColor.RED + translationManager.getMessage("worldedit_no_selection"));
                return;
            }

            BlockVector3 minPoint = selection.getMinimumPoint();
            BlockVector3 maxPoint = selection.getMaximumPoint();

            Location pos1 = new Location(player.getWorld(), minPoint.getX(), minPoint.getY(), minPoint.getZ());
            Location pos2 = new Location(player.getWorld(), maxPoint.getX(), maxPoint.getY(), maxPoint.getZ());

            restrictedRegionsManager.addRestrictedRegion("weather_events", pos1, pos2);
            player.sendMessage(ChatColor.GREEN + translationManager.getMessage("weather_events_restricted"));
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("error_restricting"));
            e.printStackTrace();
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

    private void handleEnableWorldCommand(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + translationManager.getMessage("usage_world"));
            return;
        }

        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("world_not_found"));
            return;
        }

        if (!disabledWorlds.contains(worldName)) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("world_not_disabled"));
            return;
        }

        disabledWorlds.remove(worldName);
        getConfig().set("disabled_worlds", disabledWorlds);
        saveConfig();

        player.sendMessage(ChatColor.GREEN + translationManager.getMessage("world_enabled_message").replace("%world%", worldName));
    }

    private void handleDisableWorldCommand(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + translationManager.getMessage("usage_world"));
            return;
        }

        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("world_not_found"));
            return;
        }

        if (disabledWorlds.contains(worldName)) {
            player.sendMessage(ChatColor.RED + translationManager.getMessage("world_already_disabled"));
            return;
        }

        disabledWorlds.add(worldName);
        getConfig().set("disabled_worlds", disabledWorlds);
        saveConfig();

        player.sendMessage(ChatColor.GREEN + translationManager.getMessage("world_disabled_message").replace("%world%", worldName));
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (isWorldDisabled(event.getWorld())) {
            event.setCancelled(true); // Prevent weather change in disabled worlds
            return;
        }

        if (event.toWeatherState()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + translationManager.getMessage("storm_brewing"));
        }
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> commands = Arrays.asList("reload", "summon", "language", "restrict", "world");

            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("language")) {
            return Arrays.asList("en", "fr", "es", "ru");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("world")) {
            return Arrays.asList("enable", "disable");
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable"))) {
            List<String> worldNames = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                worldNames.add(world.getName());
            }
            return worldNames;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("restrict")) {
            return Arrays.asList("meteor_shower", "meteor_impact", "treasure_meteor", "hurricane_winds", "hailstorm", "aurora_storm", "weather_events");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            return Arrays.asList("meteor_shower", "meteor_impact", "treasure_meteor", "hurricane_winds", "hailstorm", "aurora_storm");
        }

        return Collections.emptyList();
    }
}