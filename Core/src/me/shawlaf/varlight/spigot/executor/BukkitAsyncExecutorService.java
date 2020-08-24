package me.shawlaf.varlight.spigot.executor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class BukkitAsyncExecutorService extends AbstractBukkitExecutor {
    public BukkitAsyncExecutorService(Plugin plugin) {
        super(plugin);
    }

    @Override
    public @NotNull <T> CompletableFuture<T> submit(@NotNull Callable<T> task) {
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
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTaskAsynchronously(plugin, t).getTaskId());

        return future;
    }

    @Override
    public @NotNull <T> CompletableFuture<T> submit(@NotNull Runnable task, T result) {
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
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTaskAsynchronously(plugin, t).getTaskId());

        return future;
    }

    @Override
    public @NotNull CompletableFuture<?> submit(@NotNull Runnable task) {
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
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTaskAsynchronously(plugin, t).getTaskId());

        return future;
    }

    @Override
    public @NotNull <T> CompletableFuture<T> submitDelayed(@NotNull Callable<T> task, Ticks delay) {
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
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, t, delay.ticks).getTaskId());

        return future;
    }

    @Override
    public @NotNull <T> CompletableFuture<T> submitDelayed(@NotNull Runnable task, T result, Ticks delay) {
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
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, t, delay.ticks).getTaskId());

        return future;
    }

    @Override
    public @NotNull CompletableFuture<?> submitDelayed(@NotNull Runnable task, Ticks delay) {
        assertNotShutdown();

        CompletableFuture<Void> future = new CompletableFuture<>();

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
        toBukkitTaskId.put(taskId, Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, t, delay.ticks).getTaskId());

        return future;
    }

}
