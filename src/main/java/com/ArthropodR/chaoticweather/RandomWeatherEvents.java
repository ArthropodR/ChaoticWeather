package com.ArthropodR.chaoticweather;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import org.bukkit.block.Biome;

import java.util.*;

public class RandomWeatherEvents {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private boolean eventInProgress = false; // Prevent overlapping events

    public RandomWeatherEvents(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startRandomEvents() {
        // Only start if random events are enabled
        if (!plugin.getConfig().getBoolean("random_events.enabled")) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (eventInProgress) return; // Skip if an event is already happening

                for (World world : Bukkit.getWorlds()) {
                    triggerRandomEvent(world);
                }
            }
        }.runTaskTimer(plugin, 0L, randomInterval()); // Schedule with random interval
    }

    private long randomInterval() {
        // Random interval between 10 and 15 minutes (in ticks)
        return (10 * 60 * 20L) + random.nextInt(5 * 60 * 20); // 10 to 15 minutes in ticks
    }

    private void triggerRandomEvent(World world) {
        // Ensure only one event happens in the interval
        if (eventInProgress) return;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;

        Player player = players.get(random.nextInt(players.size()));
        eventInProgress = true; // Mark event as in progress

        boolean isInIcyBiome = isInIcyBiome(player);

        if (plugin.getConfig().getBoolean("random_events.meteor_shower") &&
                random.nextDouble() < plugin.getConfig().getDouble("random_events.meteor_shower_chance")) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "A meteor shower begins!");
            startMeteorShower(player);
        } else if (plugin.getConfig().getBoolean("random_events.meteor_impact") &&
                random.nextDouble() < plugin.getConfig().getDouble("random_events.meteor_impact_chance")) {
            player.sendMessage(ChatColor.RED + "A meteor strikes the earth!");
            spawnMeteorImpact(player);
        } else if (plugin.getConfig().getBoolean("random_events.treasure_meteor") &&
                random.nextDouble() < plugin.getConfig().getDouble("random_events.treasure_meteor_chance")) {
            player.sendMessage(ChatColor.GOLD + "A Treasure Meteor has landed nearby!");
            spawnTreasureMeteor(player);
        } else if (plugin.getConfig().getBoolean("random_events.hurricane_winds") &&
                random.nextDouble() < plugin.getConfig().getDouble("random_events.hurricane_winds_chance")) {
            player.sendMessage(ChatColor.BLUE + "A sudden gust of wind pushes you!");
            HurricaneWinds(player);
        } else if (isInIcyBiome) {
            // Include icy biome-specific events if in an icy biome
            if (plugin.getConfig().getBoolean("random_events.hailstorm") &&
                    random.nextDouble() < plugin.getConfig().getDouble("random_events.hailstorm_chance")) {
                if (player.getLocation().getY() > 40) {
                    player.sendMessage(ChatColor.AQUA + "A hailstorm freezes the world around you!");
                    spawnIceHazards(player);
                }
            } else if (plugin.getConfig().getBoolean("random_events.auroraStorm") &&
                    random.nextDouble() < plugin.getConfig().getDouble("random_events.auroraStorm_chance")) {
                player.sendMessage(ChatColor.GREEN + "A rare Aurora Storm lights up the sky!");
                startAuroraStorm(world);
            }
        }

        // Schedule eventInProgress reset after the interval
        new BukkitRunnable() {
            @Override
            public void run() {
                eventInProgress = false;
            }
        }.runTaskLater(plugin, randomInterval());
    }

    private boolean isInIcyBiome(Player player) {
        // Specific icy biomes where events can occur
        Biome biome = player.getLocation().getBlock().getBiome();
        return biome == Biome.ICE_SPIKES ||
                biome == Biome.SNOWY_PLAINS ||
                biome == Biome.SNOWY_SLOPES ||
                biome == Biome.FROZEN_PEAKS ||
                biome == Biome.FROZEN_RIVER ||
                biome == Biome.FROZEN_OCEAN;
    }

    public void summonEvent(Player player, String eventName) {
        switch (eventName.toLowerCase()) {
            case "meteor_shower":
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Summoning a Meteor Shower!");
                startMeteorShower(player);
                break;
            case "meteor_impact":
                player.sendMessage(ChatColor.RED + "Summoning a Meteor Impact!");
                spawnMeteorImpact(player);
                break;
            case "treasure_meteor":
                player.sendMessage(ChatColor.GOLD + "Summoning a Treasure Meteor!");
                spawnTreasureMeteor(player);
                break;
            case "hurricane_winds":
                player.sendMessage(ChatColor.BLUE + "Summoning Hurricane Winds!");
                HurricaneWinds(player);
                break;
            case "hailstorm":
                player.sendMessage(ChatColor.AQUA + "Summoning a Hailstorm!");
                hailstorm(player);
                break;
            case "aurora_storm":
                player.sendMessage(ChatColor.GREEN + "Summoning an Aurora Storm!");
                startAuroraStorm(player.getWorld());
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown event: " + eventName);
                break;
        }
    }

    // Start meteor shower for a player
    private void startMeteorShower(Player player) {
        new BukkitRunnable() {
            private int ticksElapsed = 0; // Track elapsed ticks
            private final int maxDurationTicks = 40 * 20; // 40 seconds in ticks

            @Override
            public void run() {
                if (ticksElapsed >= maxDurationTicks) { // Stop after 40 seconds
                    cancel();
                    return;
                }

                for (int i = 0; i < 30; i++) { // Spawns 30 fireballs
                    // Randomized location for the start of the fireball (near the player in the sky)
                    Location skyStart = player.getLocation().add(random.nextInt(40) - 20, 30, random.nextInt(40) - 20);

                    // Random direction for the fireball's path, making it move in a random direction relative to the player
                    double angle = random.nextDouble() * Math.PI * 2; // Random angle
                    double speed = 0.4 + random.nextDouble() * 0.2; // Random speed
                    Vector direction = new Vector(Math.cos(angle) * speed, -0.5, Math.sin(angle) * speed); // Direction based on angle and speed

                    // Create fireball entity
                    Fireball fireball = (Fireball) player.getWorld().spawnEntity(skyStart, EntityType.FIREBALL);
                    fireball.setDirection(direction);

                    // Ensure fireball has no explosion, no damage, and no fire
                    fireball.setIsIncendiary(false);
                    fireball.setYield(0);

                    // Make the fireball pass harmlessly through the sky
                    fireball.setGravity(false);
                    fireball.setSilent(true);
                }

                ticksElapsed++; // Increment elapsed time
            }
        }.runTaskTimer(plugin, 0L, 1L); // Fireballs every tick (continuous spawn)
    }

    private void spawnMeteorImpact(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Random random = new Random();

                // Randomize angle for consistent 30-block distance
                double angle = random.nextDouble() * Math.PI * 2; // Random angle in radians
                double offsetX = Math.cos(angle) * 60;
                double offsetZ = Math.sin(angle) * 60;

                Location impactLocation = player.getLocation().clone().add(offsetX, 0, offsetZ);

                // Adjust to the highest block at the location
                impactLocation = player.getWorld().getHighestBlockAt(impactLocation).getLocation();

                // Huge explosion effects
                impactLocation.getWorld().playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 6.0f, 0.5f);
                impactLocation.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, impactLocation, 20, 3.0, 3.0, 3.0, 0.1);

                createRealisticCrater(impactLocation);
            }
        }.runTaskLater(plugin, random.nextInt(100));
    }

    private void createRealisticCrater(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        int radius = 6; // Crater radius
        int depth = 4;  // Crater depth
        Random random = new Random();

        // Destroy blocks above the crater
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) continue;

                Location blockLocation = location.clone().add(x, 0, z);
                for (int y = blockLocation.getBlockY() + 1; y < blockLocation.getWorld().getMaxHeight(); y++) {
                    Location aboveLocation = blockLocation.clone();
                    aboveLocation.setY(y);  // Set Y coordinate separately
                    Block aboveBlock = world.getBlockAt(aboveLocation);
                    aboveBlock.setType(Material.AIR);
                }
            }
        }

        // Create crater
        for (int y = 0; y >= -depth; y--) {
            int currentRadius = radius - Math.abs(y);

            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int z = -currentRadius; z <= currentRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance > currentRadius) continue;

                    Location blockLocation = location.clone().add(x, y, z);
                    Block block = world.getBlockAt(blockLocation);

                    if (y == 0 && block.getType() != Material.GRASS_BLOCK && block.getType() != Material.DIRT && block.getType() != Material.STONE) {
                        continue;
                    }

                    if (distance > currentRadius - 1) {
                        block.setType(random.nextDouble() < 0.3 ? Material.NETHERRACK : Material.BLACKSTONE);
                        if (random.nextDouble() < 0.15) {
                            blockLocation.clone().add(0, 1, 0).getBlock().setType(Material.FIRE);
                        }
                    } else {
                        if (y == 0) {
                            block.setType(Material.AIR);
                        } else if (y > -depth) {
                            block.setType(random.nextDouble() < 0.2 ? Material.MAGMA_BLOCK : Material.NETHERRACK);
                        } else {
                            block.setType(random.nextDouble() < 0.5 ? Material.LAVA : Material.OBSIDIAN);
                        }
                    }
                }
            }
        }

        // Add ores
        int goldCount = 0;
        int ironBlockCount = 0;
        int ironOreCount = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) continue;

                Location blockLocation = location.clone().add(x, -random.nextInt(depth), z);
                if (goldCount < 1 && random.nextDouble() < 0.1) {
                    blockLocation.getBlock().setType(Material.GOLD_BLOCK);
                    goldCount++;
                } else if (ironBlockCount < 2 && random.nextDouble() < 0.2) {
                    blockLocation.getBlock().setType(Material.IRON_BLOCK);
                    ironBlockCount++;
                } else if (ironOreCount < 5 && random.nextDouble() < 0.48) {
                    blockLocation.getBlock().setType(Material.IRON_ORE);
                    ironOreCount++;
                }
            }
        }

        // Add smoke particles
        for (int i = 0; i < 40; i++) {
            double offsetX = random.nextDouble() * radius * 2 - radius;
            double offsetZ = random.nextDouble() * radius * 2 - radius;
            location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location.clone().add(offsetX, 1, offsetZ), 10, 0.5, 0.5, 0.5, 0.01);
        }
    }

    private void spawnTreasureMeteor(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Random random = new Random();
                double angle = random.nextDouble() * Math.PI * 2;
                double offsetX = Math.cos(angle) * 60;
                double offsetZ = Math.sin(angle) * 60;

                Location impactLocation = player.getLocation().clone().add(offsetX, 0, offsetZ);
                impactLocation = player.getWorld().getHighestBlockAt(impactLocation).getLocation();

                // Huge explosion effects
                impactLocation.getWorld().playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 6.0f, 0.5f);
                impactLocation.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, impactLocation, 20, 3.0, 3.0, 3.0, 0.1);

                createTreasureCrater(impactLocation);
            }
        }.runTaskLater(plugin, new Random().nextInt(100));
    }

    private void createTreasureCrater(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        int radius = 6;
        int depth = 7;
        Random random = new Random();

        // Destroy blocks above the crater
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) continue;

                Location blockLocation = location.clone().add(x, 0, z);
                for (int y = blockLocation.getBlockY() + 1; y < blockLocation.getWorld().getMaxHeight(); y++) {
                    Location aboveLocation = blockLocation.clone();
                    aboveLocation.setY(y);
                    Block aboveBlock = world.getBlockAt(aboveLocation);
                    aboveBlock.setType(Material.AIR);
                }
            }
        }

        // Create crater
        for (int y = 0; y >= -depth; y--) {
            int currentRadius = radius - Math.abs(y);

            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int z = -currentRadius; z <= currentRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance > currentRadius) continue;

                    Location blockLocation = location.clone().add(x, y, z);
                    Block block = world.getBlockAt(blockLocation);

                    if (y == 0 && block.getType() != Material.GRASS_BLOCK && block.getType() != Material.DIRT && block.getType() != Material.STONE) {
                        continue;
                    }

                    if (distance > currentRadius - 1) {
                        block.setType(random.nextDouble() < 0.3 ? Material.NETHERRACK : Material.BLACKSTONE);
                        if (random.nextDouble() < 0.15) {
                            blockLocation.clone().add(0, 1, 0).getBlock().setType(Material.FIRE);
                        }
                    } else {
                        if (y == 0) {
                            block.setType(Material.AIR);
                        } else if (y > -depth) {
                            block.setType(random.nextDouble() < 0.2 ? Material.MAGMA_BLOCK : Material.NETHERRACK);
                        } else {
                            block.setType(random.nextDouble() < 0.5 ? Material.LAVA : Material.OBSIDIAN);
                        }
                    }
                }
            }
        }

        // Add raised rims
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) continue;

                Location rimLocation = location.clone().add(x, 0, z);
                if (distance > radius - 2) {
                    rimLocation.getBlock().setType(Material.BLACKSTONE);
                    if (random.nextDouble() < 0.3) {
                        rimLocation.clone().add(0, 1, 0).getBlock().setType(Material.FIRE);
                    }
                }
            }
        }

        // Smoke particles
        for (int i = 0; i < 40; i++) {
            double offsetX = random.nextDouble() * radius * 2 - radius;
            double offsetZ = random.nextDouble() * radius * 2 - radius;
            location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location.clone().add(offsetX, 1, offsetZ), 10, 0.5, 0.5, 0.5, 0.01);
        }

        // Place chest above the central block at the lowest point
        Location chestLocation = location.clone().add(0, -depth + 1, 0);
        chestLocation.getBlock().setType(Material.CHEST);
        Block chestBlock = chestLocation.getBlock();
        if (chestBlock.getState() instanceof org.bukkit.block.Chest) {
            org.bukkit.block.Chest chest = (org.bukkit.block.Chest) chestBlock.getState();
            fillChestWithLoot(chest);
        }
    }

    private void fillChestWithLoot(org.bukkit.block.Chest chest) {
        Random random = new Random();
        List<ItemStack> items = new ArrayList<>();

        // Tier 1 (Rare)
        if (random.nextDouble() < 0.14) items.add(new ItemStack(Material.DIAMOND, random.nextInt(3) + 1));
        if (random.nextDouble() < 0.05) items.add(new ItemStack(Material.DIAMOND_HORSE_ARMOR, 1));
        if (random.nextDouble() < 0.14) items.add(new ItemStack(Material.GOLDEN_HORSE_ARMOR, 1));

        // Tier 2 (Uncommon)
        if (random.nextDouble() < 0.27) items.add(new ItemStack(Material.GOLD_INGOT, random.nextInt(6) + 1));
        if (random.nextDouble() < 0.20) items.add(new ItemStack(Material.IRON_HORSE_ARMOR, 1));
        if (random.nextDouble() < 0.48) items.add(new ItemStack(Material.IRON_INGOT, random.nextInt(7) + 1));

        // Tier 3 (Common)
        if (random.nextDouble() < 0.40) items.add(new ItemStack(Material.SADDLE, 1));
        if (random.nextDouble() < 0.69) items.add(new ItemStack(Material.STICK, random.nextInt(15) + 1));
        if (random.nextDouble() < 0.82) items.add(new ItemStack(Material.COAL, random.nextInt(11) + 1));

        // PvP Items (Golden)
        if (random.nextDouble() < 0.40) items.add(createGoldenItem(Material.GOLDEN_SWORD, 1));
        if (random.nextDouble() < 0.40) items.add(createGoldenItem(Material.GOLDEN_AXE, 1));
        if (random.nextDouble() < 0.50) items.add(createGoldenItem(Material.GOLDEN_HOE, 1));
        if (random.nextDouble() < 0.40) items.add(createGoldenArmorItem(Material.GOLDEN_HELMET, 1));
        if (random.nextDouble() < 0.40) items.add(createGoldenArmorItem(Material.GOLDEN_CHESTPLATE, 1));
        if (random.nextDouble() < 0.40) items.add(createGoldenArmorItem(Material.GOLDEN_LEGGINGS, 1));
        if (random.nextDouble() < 0.40) items.add(createGoldenArmorItem(Material.GOLDEN_BOOTS, 1));

        // Food Items
        if (random.nextDouble() < 0.50) items.add(new ItemStack(Material.BREAD, random.nextInt(3) + 6));
        if (random.nextDouble() < 0.15) items.add(new ItemStack(Material.GOLDEN_APPLE, random.nextInt(2) + 1));
        if (random.nextDouble() < 0.25) items.add(new ItemStack(Material.GOLDEN_CARROT, random.nextInt(2) + 4));
        if (random.nextDouble() < 0.30) items.add(new ItemStack(Material.CAKE, 1));
        if (random.nextDouble() < 0.35) items.add(new ItemStack(Material.SALMON, random.nextInt(4) + 5));
        if (random.nextDouble() < 0.20) items.add(new ItemStack(Material.COOKED_SALMON, random.nextInt(2) + 4));

        // Sticks
        if (random.nextDouble() < 0.40) items.add(new ItemStack(Material.STICK, random.nextInt(11) + 10));

        // Make sure to place exactly 14 items in the chest
        if (items.size() > 14) {
            Collections.shuffle(items);
            items = items.subList(0, 14); // Keep only the first 14 items
        }

        // Place items in random slots
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < chest.getInventory().getSize(); i++) {
            availableSlots.add(i);
        }
        Collections.shuffle(availableSlots);

        for (int i = 0; i < items.size(); i++) {
            chest.getInventory().setItem(availableSlots.get(i), items.get(i));
        }
    }

    // Helper method to create golden items (PvP)
    private ItemStack createGoldenItem(Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Randomly apply curse or sharpness (for sword and axe)
            if (material == Material.GOLDEN_SWORD || material == Material.GOLDEN_AXE) {
                if (new Random().nextDouble() < 0.50) {
                    meta.addEnchant(Enchantment.DAMAGE_ALL, 1, true); // Sharpness I
                } else {
                    meta.addEnchant(Enchantment.LURE, 1, true); // Curse of Lure as an example
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // Helper method to create golden armor items with protection or curse
    private ItemStack createGoldenArmorItem(Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Randomly apply curse or protection 2
            if (new Random().nextDouble() < 0.50) {
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true); // Protection II
            } else {
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true); // Curse of Binding
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void startAuroraStorm(World world) {
        new BukkitRunnable() {
            private int auroraDuration = 0;

            @Override
            public void run() {
                // Check if it is nighttime
                long time = world.getTime();
                if (time < 13000 || time > 23000) {
                    // If not nighttime, skip this cycle
                    return;
                }

                if (auroraDuration >= 12) {
                    cancel();
                    eventInProgress = false; // Reset event flag
                    return;
                }

                for (Player player : world.getPlayers()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0));
                }

                auroraDuration++;
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    private void HurricaneWinds(Player player) {
        Vector wind = new Vector(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5).normalize().multiply(2);
        player.setVelocity(wind);
    }

    public void spawnIceHazards(Player player) {
        new BukkitRunnable() {
            int ticksElapsed = 0; // Track elapsed ticks (20 ticks = 1 second)

            @Override
            public void run() {
                // Stop the effect after 10 seconds (200 ticks)
                if (ticksElapsed >= 200) {
                    this.cancel();
                    return;
                }

                Location location = player.getLocation();

                // Check if the player is above Y-level 64 and in the specified biomes
                if (location.getY() > 64) {
                    Biome biome = location.getWorld().getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    if (biome == Biome.ICE_SPIKES ||
                            biome == Biome.SNOWY_PLAINS ||
                            biome == Biome.SNOWY_SLOPES ||
                            biome == Biome.FROZEN_PEAKS ||
                            biome == Biome.FROZEN_RIVER ||
                            biome == Biome.FROZEN_OCEAN) {

                        // Spawn a snowball above the player's location
                        location.getWorld().spawnEntity(location.add(0, 1, 0), EntityType.SNOWBALL);

                        // Inflict half a heart of damage to the player
                        player.damage(1.0);
                    }
                }

                ticksElapsed += 20; // Increment elapsed ticks by 20 (1 second)
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void hailstorm(Player player) {
        new BukkitRunnable() {
            int ticksElapsed = 0; // Track elapsed ticks (20 ticks = 1 second)

            @Override
            public void run() {
                // Get the player's current biome
                Biome biome = player.getLocation().getBlock().getBiome();

                // Check if the player is in one of the icy biomes
                if (biome == Biome.ICE_SPIKES ||
                        biome == Biome.SNOWY_PLAINS ||
                        biome == Biome.SNOWY_SLOPES ||
                        biome == Biome.FROZEN_PEAKS ||
                        biome == Biome.FROZEN_RIVER ||
                        biome == Biome.FROZEN_OCEAN) {

                    // Stop the effect after 10 seconds (200 ticks)
                    if (ticksElapsed >= 200) {
                        this.cancel();
                        return;
                    }

                    Location location = player.getLocation();

                    // Spawn a snowball above the player's location
                    location.getWorld().spawnEntity(location.add(0, 1, 0), EntityType.SNOWBALL);

                    // Inflict half a heart of damage to the player
                    player.damage(1.0);

                    ticksElapsed += 20; // Increment elapsed ticks by 20 (1 second)
                } else {
                    // If not in an icy biome, cancel the effect
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}