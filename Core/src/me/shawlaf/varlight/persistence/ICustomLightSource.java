package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Material;

public interface ICustomLightSource {

    IntPosition getPosition();

    Material getType();

    boolean isMigrated();

    int getCustomLuminance();

}
