package me.florian.varlight;

import org.bukkit.Location;

import java.util.Objects;

public class RegionCoordinates {

    private final int regionX, regionZ;

    public RegionCoordinates(Location location) {
        this((location.getBlockX() >> 4) >> 5, (location.getBlockZ() >> 4) >> 5);
    }

    public RegionCoordinates(IntPosition intPosition) {
        this((intPosition.getX() >> 4) >> 5, (intPosition.getZ() >> 4) >> 5);
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
        return Objects.hash(regionX, regionZ);
    }
}
