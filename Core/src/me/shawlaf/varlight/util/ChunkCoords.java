package me.shawlaf.varlight.util;

import java.util.Objects;

public class ChunkCoords {

    public static final ChunkCoords ORIGIN = new ChunkCoords(0, 0);

    public final int x, z;

    public ChunkCoords(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getRegionX() {
        return x >> 5;
    }

    public int getRegionRelativeX() {
        return ((x % 32) + 32) % 32;
    }

    public int getRegionZ() {
        return z >> 5;
    }

    public int getRegionRelativeZ() {
        return ((z % 32) + 32) % 32;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkCoords that = (ChunkCoords) o;
        return x == that.x &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "ChunkCoords{" +
                "x=" + x +
                ", z=" + z +
                '}';
    }

    public RegionCoords toRegionCoords() {
        return new RegionCoords(getRegionX(), getRegionZ());
    }
}
