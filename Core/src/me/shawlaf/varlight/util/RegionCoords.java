package me.shawlaf.varlight.util;

import org.bukkit.Location;

public class RegionCoords {

    public final int x, z;

    public RegionCoords(Location location) {
        this(new IntPosition(location));
    }

    public RegionCoords(IntPosition intPosition) {
        this(intPosition.getChunkX() >> 5, intPosition.getChunkZ() >> 5);
    }

    public RegionCoords(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionCoords that = (RegionCoords) o;
        return x == that.x &&
                z == that.z;
    }

    @Override
    public int hashCode() {
        int result = 89 * 113 + x;
        return 89 * result + z;
    }

    @Override
    public String toString() {
        return String.format("%d %d", x, z);
    }
}
