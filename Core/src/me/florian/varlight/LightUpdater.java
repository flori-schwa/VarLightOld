package me.florian.varlight;

import org.bukkit.Location;

public interface LightUpdater {

    void setLight(Location location, int lightLevel);

    default int getChunkBitMask(Location location) {
        int sectionY = location.getBlockY() / 16;
        int mask = 1 << sectionY;

        if (sectionY == 0) {
            return mask | 2;
        }

        if (sectionY == 15) {
            return mask | 0x4000;
        }

        return mask | (1 << (sectionY - 1)) | (1 << (sectionY + 1));
    }
}
