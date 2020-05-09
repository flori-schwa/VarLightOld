package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class AutosaveManager implements Listener {

    private final VarLightPlugin plugin;

    private BukkitTask autosaveTask;
    private boolean isPersistOnSave = false;

    public AutosaveManager(VarLightPlugin plugin) {
        this.plugin = plugin;

        update(plugin.getConfiguration().getAutosaveInterval());
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        if (!isPersistOnSave) {
            return;
        }

        WorldLightSourceManager manager = plugin.getManager(e.getWorld());

        if (manager != null) {
            manager.save(Bukkit.getConsoleSender(), plugin.getConfiguration().isLogVerbose());
        }
    }

    public void update(int interval) {
        if (autosaveTask != null && !autosaveTask.isCancelled()) {
            autosaveTask.cancel();
            autosaveTask = null;
        }

        if (interval == 0) {
            plugin.getLogger().warning("VarLight Autosave is disabled! You must save Light sources manually");
            isPersistOnSave = false;

            return;
        }

        if (interval < 0) {
            plugin.getLogger().info("Persist on world save enabled");
            isPersistOnSave = true;

            return;
        }

        plugin.getLogger().info("Automatically saving all light sources every " + interval + " minutes");

        final long ticks = VarLightPlugin.TICK_RATE * TimeUnit.MINUTES.toSeconds(interval);

        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> {
                    for (WorldLightSourceManager manager : plugin.getAllManagers()) {
                        manager.save(Bukkit.getConsoleSender(), plugin.getConfiguration().isLogVerbose());
                    }
                },
                ticks, ticks
        );
    }


}
