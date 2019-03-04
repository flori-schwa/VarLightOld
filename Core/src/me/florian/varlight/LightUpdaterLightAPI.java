package me.florian.varlight;

import org.bukkit.Location;
import ru.beykerykt.lightapi.LightAPI;

public class LightUpdaterLightAPI implements LightUpdater {

    @Override
    public void setLight(Location location, int lightLevel) {
        LightAPI.deleteLight(location, false);

        if (lightLevel > 0) {
            LightAPI.createLight(location, lightLevel, false);
        }

        LightAPI.updateChunks(location, location.getWorld().getPlayers());
        return;
    }
}
