package com.ArthropodR.chaoticweather;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

public class WorldEditUtils {
    private static WorldEditPlugin getWorldEdit() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            return null;
        }
        return (WorldEditPlugin) plugin;
    }

    public static Location getSelectionPos1(Player player) {
        WorldEditPlugin worldEdit = getWorldEdit();
        if (worldEdit == null) return null;

        try {
            LocalSession session = worldEdit.getSession(player);
            Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            return BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint());
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    public static Location getSelectionPos2(Player player) {
        WorldEditPlugin worldEdit = getWorldEdit();
        if (worldEdit == null) return null;

        try {
            LocalSession session = worldEdit.getSession(player);
            Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            return BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint());
        } catch (IncompleteRegionException e) {
            return null;
        }
    }
}