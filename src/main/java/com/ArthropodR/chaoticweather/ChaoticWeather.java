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
    private com.ArthropodR.chaoticweather.WeatherEvents weatherEvents;
    private RandomWeatherEvents randomWeatherEvents;

    @Override
    public void onEnable() {
        // Ensure default config is saved if it doesn't exist
        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Chaotic Weather plugin has been enabled!");

        weatherEvents = new com.ArthropodR.chaoticweather.WeatherEvents(this);
        randomWeatherEvents = new RandomWeatherEvents(this);

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
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chaoticweather.summon")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /chaoticweather <reload | summon> [event]");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("chaoticweather.reload")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to reload the plugin.");
                return true;
            }

            reloadConfig();
            player.sendMessage(ChatColor.GREEN + "ChaoticWeather configuration reloaded.");
            return true;
        } else if (args[0].equalsIgnoreCase("summon")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /chaoticweather summon <event>");
                return true;
            }

            String eventName = args[1].toLowerCase();

            // Trigger event from RandomWeatherEvents
            randomWeatherEvents.summonEvent(player, eventName);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown command. Usage: /chaoticweather <reload | summon>");
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "summon");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            return Arrays.asList("meteor_shower", "meteor_impact", "treasure_meteor", "hurricane_winds", "hailstorm", "aurora_storm");
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
            Bukkit.broadcastMessage(ChatColor.GOLD + "Chaotic Weather: " + ChatColor.AQUA + "A storm is brewing!");
        }
    }
}