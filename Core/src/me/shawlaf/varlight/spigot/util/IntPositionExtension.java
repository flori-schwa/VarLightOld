package me.shawlaf.varlight.spigot.util;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@UtilityClass
public class IntPositionExtension {
    public static IntPosition toIntPosition(Location location) {
        return new IntPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static IntPosition toIntPosition(Block block) {
        return new IntPosition(block.getX(), block.getY(), block.getZ());
    }

    public static Location toLocation(IntPosition position, World world) {
        return new Location(world, position.x, position.y, position.z);
    }

    public static Block toBlock(IntPosition position, World world) {
        return world.getBlockAt(position.x, position.y, position.z);
    }
}
