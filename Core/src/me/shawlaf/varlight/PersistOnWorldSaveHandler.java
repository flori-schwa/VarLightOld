package me.shawlaf.varlight;

import me.shawlaf.varlight.persistence.WorldLightSourceManager;
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
        WorldLightSourceManager manager = plugin.getManager(e.getWorld());

        if (manager != null) {
            manager.save(Bukkit.getConsoleSender());
        }
    }

}
