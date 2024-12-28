package com.ArthropodR.chaoticweather;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RestrictedRegionsManager {

    private final JavaPlugin plugin;
    private final File configFile;
    private final FileConfiguration config;

    // Map to store restricted regions categorized by event name
    private final Map<String, List<BoundingBox>> restrictedRegions = new HashMap<>();

    public RestrictedRegionsManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Initialize configuration file for storing restricted regions
        this.configFile = new File(plugin.getDataFolder(), "restricted_regions.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create restricted_regions.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // Load regions from the file
        loadRestrictedRegions();
    }

    /**
     * Add a restricted region for a specific event.
     *
     * @param eventName The name of the event.
     * @param pos1      The first corner of the region.
     * @param pos2      The opposite corner of the region.
     */
    public void addRestrictedRegion(String eventName, Location pos1, Location pos2) {
        BoundingBox box = BoundingBox.of(pos1, pos2);
        restrictedRegions.computeIfAbsent(eventName.toLowerCase(), k -> new ArrayList<>()).add(box);
        saveRestrictedRegion(eventName, box);
    }

    /**
     * Check if a location is within any restricted region for a specific event.
     *
     * @param eventName The name of the event.
     * @param location  The location to check.
     * @return True if the location is restricted; otherwise, false.
     */
    public boolean isRestricted(String eventName, Location location) {
        List<BoundingBox> regions = restrictedRegions.get(eventName.toLowerCase());
        if (regions == null) return false;

        for (BoundingBox box : regions) {
            if (box.contains(location.toVector())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clear all restricted regions for a specific event.
     *
     * @param eventName The name of the event.
     */
    public void clearRestrictedRegions(String eventName) {
        restrictedRegions.remove(eventName.toLowerCase());
        config.set("regions." + eventName.toLowerCase(), null);
        saveConfig();
    }

    /**
     * List all restricted regions for a specific event.
     *
     * @param eventName The name of the event.
     * @return A list of bounding boxes for the event's restricted regions.
     */
    public List<BoundingBox> getRestrictedRegionsForEvent(String eventName) {
        return restrictedRegions.getOrDefault(eventName.toLowerCase(), new ArrayList<>());
    }

    /**
     * Get all restricted regions for debugging or informational purposes.
     *
     * @return A map of event names to their restricted regions.
     */
    public Map<String, List<BoundingBox>> getAllRestrictedRegions() {
        return restrictedRegions;
    }

    /**
     * Load restricted regions from the configuration file.
     */
    public void loadRestrictedRegions() {
        restrictedRegions.clear();
        if (config.contains("regions")) {
            for (String eventName : config.getConfigurationSection("regions").getKeys(false)) {
                List<Map<String, Object>> regions = (List<Map<String, Object>>) config.get("regions." + eventName);
                List<BoundingBox> boundingBoxes = new ArrayList<>();

                for (Map<String, Object> region : regions) {
                    Location pos1 = deserializeLocation((Map<String, Object>) region.get("pos1"));
                    Location pos2 = deserializeLocation((Map<String, Object>) region.get("pos2"));
                    if (pos1 != null && pos2 != null) {
                        BoundingBox box = BoundingBox.of(pos1, pos2);
                        boundingBoxes.add(box);
                    }
                }
                restrictedRegions.put(eventName.toLowerCase(), boundingBoxes);
            }
        }
    }

    /**
     * Save a new restricted region to the configuration file.
     *
     * @param eventName The name of the event.
     * @param box       The bounding box of the region.
     */
    private void saveRestrictedRegion(String eventName, BoundingBox box) {
        List<Map<String, Object>> regions = (List<Map<String, Object>>) config.get("regions." + eventName.toLowerCase());
        if (regions == null) regions = new ArrayList<>();

        Map<String, Object> regionData = new HashMap<>();
        regionData.put("pos1", serializeLocation(box.getMin().toLocation(getWorld())));
        regionData.put("pos2", serializeLocation(box.getMax().toLocation(getWorld())));
        regions.add(regionData);

        config.set("regions." + eventName.toLowerCase(), regions);
        saveConfig();
    }

    /**
     * Save the configuration file.
     */
    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save restricted_regions.yml: " + e.getMessage());
        }
    }

    /**
     * Serialize a location into a map for storage.
     *
     * @param location The location to serialize.
     * @return A map representation of the location.
     */
    private Map<String, Object> serializeLocation(Location location) {
        return location.serialize();
    }

    /**
     * Deserialize a location from a map.
     *
     * @param data The map containing location data.
     * @return The deserialized location.
     */
    private Location deserializeLocation(Map<String, Object> data) {
        return Location.deserialize(data);
    }

    /**
     * Get the main world or fallback world for BoundingBox operations.
     *
     * @return The world object.
     */
    private World getWorld() {
        return Bukkit.getWorlds().get(0); // Main or fallback world
    }
}