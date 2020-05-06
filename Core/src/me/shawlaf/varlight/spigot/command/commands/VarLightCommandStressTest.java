package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.spigot.util.LightSourceUtil;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.success;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandStressTest extends VarLightSubCommand {

    private BukkitTask currentTask;

    public VarLightCommandStressTest(VarLightCommand command) {
        super(command, "stresstest");
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.requires(c -> c instanceof Player);

        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("start")
                        .executes(this::runStart)
        );

        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("stop")
                        .executes(this::runStop)
        );

        return node;
    }

    private static <T> boolean containsArray(T[] array, T element) {
        for (T t : array) {
            if (Objects.equals(t, element)) {
                return true;
            }
        }

        return false;
    }

    private int runStart(CommandContext<CommandSender> context) {
        synchronized (this) {
            if (currentTask != null) {
                failure(this, context.getSource(), "Already running a stress test");
                return FAILURE;
            }

            Player player = (Player) context.getSource();

            World world = player.getWorld();
            WorldLightSourceManager manager = plugin.getManager(world);

            if (manager == null) {
                failure(this, player, "VarLight not enabled in your world");
                return FAILURE;
            }

            currentTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (!player.isValid() || !player.getWorld().equals(world)) {
                    currentTask.cancel();
                    return;
                }

                IntPosition center = IntPositionExtension.toIntPosition(player.getLocation());

                final IntPosition[] toPlace = new IntPosition[ThreadLocalRandom.current().nextInt(1, 16)];

                IntPosition tmp;

                for (int i = 0; i < toPlace.length; i++) {
                    do {
                        int dx = (ThreadLocalRandom.current().nextBoolean() ? -1 : 1) * ThreadLocalRandom.current().nextInt(100);
                        int dz = (ThreadLocalRandom.current().nextBoolean() ? -1 : 1) * ThreadLocalRandom.current().nextInt(100);
                        int dy = (ThreadLocalRandom.current().nextBoolean() ? -1 : 1) * ThreadLocalRandom.current().nextInt(100);

                        tmp = center.getRelative(dx, -center.y + dy, dz);
                    } while ((plugin.getNmsAdapter().isIllegalBlock(IntPositionExtension.toBlock(tmp, player.getWorld())) || manager.getCustomLuminance(tmp, 0) > 0) && !containsArray(toPlace, tmp));

                    toPlace[i] = tmp;
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(30));
                    } catch (InterruptedException e) {
                        // Ignore
                    }

                    for (IntPosition position : toPlace) {
                        if (LightSourceUtil.placeNewLightSource(plugin, IntPositionExtension.toLocation(position, world), 15).successful()) {
                            player.sendMessage("[Stress Test] Placed LS at " + position.toShortString());
                        }
                    }
                });
            }, 1L, 1L);

            success(this, context.getSource(), "Started Stress testing");
        }

        return SUCCESS;
    }

    private int runStop(CommandContext<CommandSender> context) {
        synchronized (this) {
            if (currentTask == null) {
                failure(this, context.getSource(), "No stress test running");
                return FAILURE;
            }

            currentTask.cancel();
            currentTask = null;

            success(this, context.getSource(), "Stopped Stress testing");
        }

        return SUCCESS;
    }
}
