package me.shawlaf.varlight.spigot.command.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldEditUtil {

    private final VarLightPlugin plugin;
    private final WorldEditPlugin worldEditPlugin;

    public WorldEditUtil(VarLightPlugin plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            throw new RuntimeException("WorldEdit not installed");
        }

        this.worldEditPlugin = JavaPlugin.getPlugin(WorldEditPlugin.class);
    }

    public Location[] getSelection(Player player, World world) {
        LocalSession session = worldEditPlugin.getSession(player);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        Region region;

        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            return null;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        Location a = new Location(player.getWorld(), min.getX(), min.getY(), min.getZ());
        Location b = new Location(player.getWorld(), max.getX(), max.getY(), max.getZ());

        return new Location[] {a, b};
    }
}
