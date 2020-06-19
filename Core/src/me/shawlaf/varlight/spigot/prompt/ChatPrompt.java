package me.shawlaf.varlight.spigot.prompt;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ChatPrompt {

    private static final Object ID_TRACK_MUTEX = new Object();
    private static int ID_TRACKER = 0;

    private final int id;

    private boolean timeout = false, completed = false, cancelled = false;

    private final VarLightPlugin plugin;
    private final BaseComponent[] message;
    private final Runnable onConfirm;

    private BukkitTask timeoutTask;

    public ChatPrompt(@NotNull VarLightPlugin plugin, @NotNull BaseComponent[] message, @NotNull Runnable onConfirm) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(message);
        Objects.requireNonNull(onConfirm);

        synchronized (ID_TRACK_MUTEX) {
            this.id = ID_TRACKER++;
        }

        this.plugin = plugin;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    public void sendMessage(CommandSender source) {
        source.spigot().sendMessage(message);
    }

    public void start(CommandSender source, long timeout, TimeUnit timeUnit) {
        synchronized (this) {
            source.spigot().sendMessage(message);

            this.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                synchronized (ChatPrompt.this) {
                    if (!completed && !cancelled) {
                        this.timeout = true;
                    }
                }
            }, timeUnit.toSeconds(timeout) * 20L);
        }
    }

    public void confirm() {
        synchronized (this) {
            if (this.timeout || this.completed || this.cancelled) {
                return;
            }

            this.timeoutTask.cancel();
            this.completed = true;
            this.onConfirm.run();
        }
    }

    public void cancel() {
        synchronized (this) {
            if (this.timeout || this.completed || this.cancelled) {
                return;
            }

            this.timeoutTask.cancel();
            this.cancelled = true;
        }
    }

    public boolean isTimeout() {
        return timeout;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isActive() {
        return !timeout && !cancelled && !completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return id == ((ChatPrompt) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
