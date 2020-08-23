package me.shawlaf.varlight.spigot.executor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BukkitSyncExecutorService extends AbstractBukkitExecutor {
    public BukkitSyncExecutorService(Plugin plugin) {
        super(plugin);
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
        assertNotShutdown();

        CompletableFuture<T> future = new CompletableFuture<>();

        int taskId = nextTaskId();

        Runnable t = () -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }

            removeTask(taskId);
        };

        tasks.put(taskId, t);
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTask(plugin, t).getTaskId());

        return future;
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        assertNotShutdown();

        CompletableFuture<T> future = new CompletableFuture<>();

        int taskId = nextTaskId();

        Runnable t = () -> {
            try {
                task.run();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }

            removeTask(taskId);
        };

        tasks.put(taskId, t);
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTask(plugin, t).getTaskId());

        return future;
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
        assertNotShutdown();

        CompletableFuture<?> future = new CompletableFuture<>();

        int taskId = nextTaskId();

        Runnable t = () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }

            removeTask(taskId);
        };

        tasks.put(taskId, t);
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTask(plugin, t).getTaskId());

        return future;
    }
}
