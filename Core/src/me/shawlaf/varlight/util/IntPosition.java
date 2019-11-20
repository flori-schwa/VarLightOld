package me.shawlaf.varlight.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import javax.swing.plaf.synth.Region;
import java.util.Objects;

public class IntPosition implements Comparable<IntPosition> {

    public static final IntPosition ORIGIN = new IntPosition(0, 0, 0);

    private final int x, y, z;

    public IntPosition(Location location) {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public IntPosition(long val) {
        this((int) (val >> 38), (int) (val & 0xFFF), (int) (val << 26 >> 38));
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

    public int getRegionX() {
        return getChunkX() >> 5;
    }

    public int getRegionZ() {
        return getChunkZ() >> 5;
    }

    public boolean outOfBounds() {
        return y < 0 || y > 255;
    }

    public int manhattanDistance(IntPosition other) {
        Objects.requireNonNull(other);

        int total = 0;

        total += Math.abs(x - other.x);
        total += Math.abs(y - other.y);
        total += Math.abs(z - other.z);

        return total;
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

    public long encode() {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) z & 0x3FFFFFF) << 12) | ((long) y & 0xFFF);
    }

    public ChunkCoords toChunkCoords() {
        return new ChunkCoords(getChunkX(), getChunkZ());
    }

    public RegionCoords toRegionCoords() {
        return new RegionCoords(getRegionX(), getRegionZ());
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

    @Override
    public int compareTo(@NotNull IntPosition o) {
        return Integer.compare(this.manhattanDistance(ORIGIN), o.manhattanDistance(ORIGIN));
    }
}
