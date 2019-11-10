package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Material;

public class BasicCustomLightSource implements ICustomLightSource {
    private final IntPosition position;
    private final String type;
    private final int emittingLight;
    private final boolean migrated;

    public BasicCustomLightSource(IntPosition position, String type, int emittingLight, boolean migrated) {
        this.position = position;
        this.type = type;
        this.emittingLight = emittingLight;
        this.migrated = migrated;
    }

    @Override
    public IntPosition getPosition() {
        return position;
    }

    @Override
    public Material getType() {
        return Material.valueOf(type);
    }

    @Override
    public boolean isMigrated() {
        return migrated;
    }

    @Override
    public int getEmittingLight() {
        return emittingLight;
    }

    @Override
    public String toString() {
        return "BasicStoredLightSource{" +
                "position=" + position +
                ", type='" + type + '\'' +
                ", emittingLight=" + emittingLight +
                ", migrated=" + migrated +
                '}';
    }
}