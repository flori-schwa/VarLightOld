package me.shawlaf.varlight.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Objects;

public class IntPosition {

    private final int x, y, z;

    public IntPosition(Location location) {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public IntPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getChunkX() {
        return x >> 4;
    }

    public int getChunkZ() {
        return z >> 4;
    }

    public boolean outOfBounds() {
        return y < 0 || y > 255;
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    public Block toBlock(World world) {
        return world.getBlockAt(x, y, z);
    }

    public IntPosition getRelative(BlockFace blockFace) {
        return new IntPosition(x + blockFace.getModX(), y + blockFace.getModY(), z + blockFace.getModZ());
    }

    public boolean isChunkLoaded(World world) {
        return world.isChunkLoaded(getChunkX(), getChunkZ());
    }

    public boolean loadChunk(World world, boolean generate) {
        return world.loadChunk(getChunkX(), getChunkZ(), generate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntPosition that = (IntPosition) o;
        return x == that.x &&
                y == that.y &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "IntPosition{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
