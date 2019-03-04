package me.florian.varlight;

import org.bukkit.Location;

public interface LightUpdater {

    void setLight(Location location, int lightLevel);

}
