package me.shawlaf.varlight.util;

import org.bukkit.Location;

public class RegionCoordinates {

    public final int x, z;

    public RegionCoordinates(Location location) {
        this(new IntPosition(location));
    }

    public RegionCoordinates(IntPosition intPosition) {
        this(intPosition.getChunkX() >> 5, intPosition.getChunkZ() >> 5);
    }

    public RegionCoordinates(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionCoordinates that = (RegionCoordinates) o;
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
