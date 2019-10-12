package me.shawlaf.varlight;

import me.shawlaf.varlight.persistence.LightSourcePersistor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

public class PersistOnWorldSaveHandler implements Listener {

    private final VarLightPlugin plugin;

    public PersistOnWorldSaveHandler(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        LightSourcePersistor persistor = LightSourcePersistor.getPersistor(plugin, e.getWorld());

        if (persistor != null) {
            persistor.save(Bukkit.getConsoleSender());
        }
    }

}
