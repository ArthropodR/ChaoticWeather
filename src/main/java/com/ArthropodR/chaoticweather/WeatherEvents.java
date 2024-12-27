package com.ArthropodR.chaoticweather;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WeatherEvents {
    private final ChaoticWeather plugin;
    private final TranslationManager translationManager;
    private final Set<Player> activeRainPlayers = new HashSet<>();
    private final Set<Player> activeThunderstormPlayers = new HashSet<>();
    private final Map<Player, Long> rainEffectsApplied = new HashMap<>();
    private final Map<Player, Long> thunderstormEffectsApplied = new HashMap<>();

    private static final long COOLDOWN_PERIOD = 10 * 60 * 1000; // 10 minutes in milliseconds

    public WeatherEvents(ChaoticWeather plugin, TranslationManager translationManager) {
        this.plugin = plugin;
        this.translationManager = translationManager;
    }

    public void startRainEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorlds().get(0).hasStorm()) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().hasStorm() && player.getLocation().getY() > 64 && !activeRainPlayers.contains(player)) {
                            long lastAppliedTime = rainEffectsApplied.getOrDefault(player, 0L);
                            if (System.currentTimeMillis() - lastAppliedTime >= COOLDOWN_PERIOD && Math.random() < plugin.getConfig().getDouble("events.rain_effects_probability", 0.5)) {
                                activeRainPlayers.add(player);
                                applyRainEffects(player);

                                rainEffectsApplied.put(player, System.currentTimeMillis());

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        activeRainPlayers.remove(player);
                                    }
                                }.runTaskLater(plugin, 1200L); // Effect lasts 60 seconds
                            }
                        }
                    }
                } else {
                    activeRainPlayers.clear();
                }
            }
        }.runTaskTimer(plugin, 0L, 200L);
    }

    public void startPlantGrowthTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getConfig().getBoolean("events.plant_growth_enhancement", true)) {
                    for (World world : Bukkit.getWorlds()) {
                        if (world.hasStorm()) {
                            enhancePlantGrowth(world);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 600L);
    }

    private void applyRainEffects(Player player) {
        if (plugin.getConfig().getBoolean("events.rain_effects", true)) {
            int randomEffect = (int) (Math.random() * 4) + 1;

            switch (randomEffect) {
                case 1:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 0));
                    player.sendMessage(ChatColor.GREEN + translationManager.getMessage("events.rain_effects.speed"));
                    break;
                case 2:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 0));
                    player.sendMessage(ChatColor.RED + translationManager.getMessage("events.rain_effects.increase_damage"));
                    break;
                case 3:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 20, 0));
                    player.sendMessage(ChatColor.GOLD + translationManager.getMessage("events.rain_effects.heal"));
                    break;
                case 4:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1200, 0));
                    player.sendMessage(ChatColor.DARK_PURPLE + translationManager.getMessage("events.rain_effects.blindness"));
                    break;
            }
        }
    }

    private void enhancePlantGrowth(World world) {
        float growthMultiplier = plugin.getConfig().getBoolean("events.plant_growth_enhancement", true) ?
                (world.isThundering() ? 0.5f : 2.0f) :
                1.0f;

        for (int x = -50; x <= 50; x++) {
            for (int z = -50; z <= 50; z++) {
                Location baseLocation = world.getSpawnLocation().add(x, 0, z);
                Block highestBlock = world.getHighestBlockAt(baseLocation);

                for (int y = highestBlock.getY(); y >= world.getMinHeight(); y--) {
                    Block block = world.getBlockAt(x, y, z);
                    if (isCrop(block)) {
                        BlockData blockData = block.getBlockData();
                        if (blockData instanceof Ageable) {
                            Ageable ageable = (Ageable) blockData;
                            if (ageable.getAge() < ageable.getMaximumAge()) {
                                int growthIncrease = (int) (growthMultiplier);
                                ageable.setAge(ageable.getAge() + growthIncrease);
                                block.setBlockData(ageable);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isCrop(Block block) {
        return block.getType() == Material.WHEAT ||
                block.getType() == Material.CARROTS ||
                block.getType() == Material.POTATOES ||
                block.getType() == Material.BEETROOTS ||
                block.getType() == Material.NETHER_WART ||
                block.getType() == Material.COCOA ||
                block.getType() == Material.SWEET_BERRY_BUSH;
    }

    public void startThunderstormEffects() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorlds().get(0).isThundering()) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().isThundering() && player.getLocation().getY() > 64 && !activeThunderstormPlayers.contains(player)) {
                            long lastAppliedTime = thunderstormEffectsApplied.getOrDefault(player, 0L);
                            if (System.currentTimeMillis() - lastAppliedTime >= COOLDOWN_PERIOD && Math.random() < plugin.getConfig().getDouble("events.thunderstorm_effects_probability", 0.5)) {
                                activeThunderstormPlayers.add(player);
                                applyThunderstormEffects(player);

                                thunderstormEffectsApplied.put(player, System.currentTimeMillis());

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        activeThunderstormPlayers.remove(player);
                                    }
                                }.runTaskLater(plugin, 1200L); // Effect lasts 60 seconds
                            }
                        }
                    }
                } else {
                    activeThunderstormPlayers.clear();
                }
            }
        }.runTaskTimer(plugin, 0L, 200L);
    }

    private void applyThunderstormEffects(Player player) {
        if (plugin.getConfig().getBoolean("events.thunderstorm_effects", true)) {
            int randomEffect = (int) (Math.random() * 3) + 1;

            switch (randomEffect) {
                case 1:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1200, 0));
                    player.sendMessage(ChatColor.DARK_GRAY + translationManager.getMessage("events.thunderstorm_effects.blindness"));
                    break;
                case 2:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1200, 0));
                    player.sendMessage(ChatColor.DARK_RED + translationManager.getMessage("events.thunderstorm_effects.slow"));
                    break;
                case 3:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 0));
                    player.sendMessage(ChatColor.GRAY + translationManager.getMessage("events.thunderstorm_effects.increase_damage"));
                    break;
            }
        }
    }
}