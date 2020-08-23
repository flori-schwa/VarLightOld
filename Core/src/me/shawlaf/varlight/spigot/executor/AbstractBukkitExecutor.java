package me.shawlaf.varlight.spigot.executor;

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
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
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

        CompletableFuture<List<Future<T>>> waitFuture = new CompletableFuture<>();

        submit(() -> {
            for (Future<T> future : futures) {
                try {
                    ((CompletableFuture<T>) future).join();
                } catch (Exception ignored) {

                }
            }

            waitFuture.complete(futures);
        });

        return waitFuture.join();
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
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

        CompletableFuture<List<Future<T>>> waitFuture = new CompletableFuture<>();

        submit(() -> {
            for (Future<T> future : futures) {
                try {
                    ((CompletableFuture<T>) future).join();
                } catch (Exception ignored) {

                }
            }

            waitFuture.complete(futures);
        });

        try {
            return waitFuture.get(timeout, unit);
        } catch (ExecutionException | TimeoutException e) {
            return futures;
        }
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
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

        return future.get();
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
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

        return future.get(timeout, unit);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        submit(command);
    }
}
