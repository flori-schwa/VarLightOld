package me.shawlaf.varlight.spigot.util;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class ProgressReport {

    public static final ProgressReport EMPTY = new ProgressReport(null, null, "", 0) {
        @Override
        public void reportProgress(int progress) {
            // Nothing
        }

        @Override
        void startTimer() {
            // Nothing
        }

        @Override
        public void finish() {
            // Nothing
        }
    };

    private final Plugin plugin;
    private final CommandSender commandSender;
    private final String name;
    private final int taskSize;
    private final long start;

    private int progress, taskId;

    public ProgressReport(Plugin plugin, CommandSender commandSender, String name, int taskSize) {
        this.plugin = plugin;
        this.commandSender = commandSender;
        this.name = name;
        this.taskSize = taskSize;
        this.start = System.currentTimeMillis();

        startTimer();
    }

    void startTimer() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            commandSender.sendMessage(String.format("[%s] %s: %.2f%%", plugin.getName(), name, 100 * ((double) this.progress / (double) taskSize)));
        }, 0, 5 * VarLightPlugin.TICK_RATE);
    }

    public void reportProgress(int progress) {
        this.progress = progress;
    }

    public void finish() {
        Bukkit.getScheduler().cancelTask(taskId);

        long end = System.currentTimeMillis();
        Duration duration = Duration.of(end - start, ChronoUnit.MILLIS);

        commandSender.sendMessage(String.format("[%s] %s: Took %d Minutes and %s Seconds", plugin.getName(), name, duration.toMinutes(), duration.getSeconds() % 60));
    }

}
