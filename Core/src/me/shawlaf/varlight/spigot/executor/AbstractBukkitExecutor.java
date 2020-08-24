package me.shawlaf.varlight.spigot.executor;

import me.shawlaf.varlight.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractBukkitExecutor implements ExecutorService {
    protected final Plugin plugin;
    protected boolean shutdown = false;
    protected CompletableFuture<Void> terminatedFuture = new CompletableFuture<>();

    protected boolean terminated = false;

    protected int nextTaskId = 0;

    protected final Map<Integer, Runnable> tasks = new HashMap<>();
    protected final Map<Integer, Integer> toBukkitTaskId = new HashMap<>();

    public AbstractBukkitExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    protected synchronized int nextTaskId() {
        return nextTaskId++;
    }

    protected void removeTask(int taskId) {
        synchronized (tasks) {
            tasks.remove(taskId);
            toBukkitTaskId.remove(taskId);

            if (shutdown && tasks.size() == 0) {
                terminated = true;
                terminatedFuture.complete(null);
            }
        }
    }

    protected void assertNotShutdown() {
        if (shutdown) {
            throw new RejectedExecutionException("This Executor Service was shut down");
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> runningTasks = new ArrayList<>();

        synchronized (tasks) {
            runningTasks.addAll(tasks.values());

            for (int bukkitTask : toBukkitTaskId.values()) {
                Bukkit.getScheduler().cancelTask(bukkitTask);
            }
        }

        return runningTasks;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        try {
            terminatedFuture.get(timeout, unit);
            return true;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) {
        return invokeAll0(tasks).item2.join();
    }

    @Override
    public <T> @NotNull List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        Tuple<List<Future<T>>, CompletableFuture<List<Future<T>>>> tuple = invokeAll0(tasks);

        try {
            return tuple.item2.get(timeout, unit);
        } catch (ExecutionException | TimeoutException e) {
            return tuple.item1;
        }
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return invokeAny0(tasks).get();
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return invokeAny0(tasks).get(timeout, unit);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        submit(command);
    }

    @Override
    public abstract <T> @NotNull CompletableFuture<T> submit(@NotNull Runnable task, T result);

    @Override
    public abstract <T> @NotNull CompletableFuture<T> submit(@NotNull Callable<T> task);

    public abstract @NotNull CompletableFuture<?> submit(@NotNull Runnable task);

    public abstract <T> @NotNull CompletableFuture<T> submitDelayed(@NotNull Callable<T> task, Ticks delay);

    public abstract <T> @NotNull CompletableFuture<T> submitDelayed(@NotNull Runnable task, T result, Ticks delay);

    public abstract @NotNull CompletableFuture<?> submitDelayed(@NotNull Runnable task, Ticks delay);

    private <T> Tuple<List<Future<T>>, CompletableFuture<List<Future<T>>>> invokeAll0(Collection<? extends Callable<T>> tasks) {
        assertNotShutdown();

        List<Future<T>> futures = new ArrayList<>(tasks.size());

        for (Callable<T> task : tasks) {
            CompletableFuture<T> future = new CompletableFuture<>();
            futures.add(future);

            submit(() -> {
                try {
                    future.complete(task.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }

        return new Tuple<>(futures, submit(() -> {
            for (Future<T> future : futures) {
                try {
                    ((CompletableFuture<T>) future).join();
                } catch (Exception ignored) {

                }
            }

            return futures;
        }));
    }

    private <T> CompletableFuture<T> invokeAny0(Collection<? extends Callable<T>> tasks) {
        assertNotShutdown();

        CompletableFuture<T> future = new CompletableFuture<>();
        AtomicInteger failCount = new AtomicInteger(0);

        for (Callable<T> task : tasks) {
            submit(() -> {

                T result;

                try {
                    result = task.call();
                } catch (Exception e) {

                    if (failCount.incrementAndGet() == tasks.size()) {
                        future.completeExceptionally(new ExecutionException(e)); // All futures have completed exceptionally
                    }

                    return; // Exception results are ignored
                }

                synchronized (future) {
                    future.complete(result);
                }
            });
        }

        return future;
    }
}
