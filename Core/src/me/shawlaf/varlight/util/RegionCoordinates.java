package me.shawlaf.varlight.util;

import org.bukkit.Location;

public class RegionCoordinates {

    private final int regionX, regionZ;

    public RegionCoordinates(Location location) {
        this(new IntPosition(location));
    }

    public RegionCoordinates(IntPosition intPosition) {
        this(intPosition.getChunkX() >> 5, intPosition.getChunkZ() >> 5);
    }

    public RegionCoordinates(int regionX, int regionZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionCoordinates that = (RegionCoordinates) o;
        return regionX == that.regionX &&
                regionZ == that.regionZ;
    }

    @Override
    public int hashCode() {
        int result = 89 * 113 + regionX;
        return 89 * result + regionZ;
    }

    @Override
    public String toString() {
        return String.format("%d %d", regionX, regionZ);
    }
}
