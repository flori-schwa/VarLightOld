package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.result.CommandResult;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class StepsizeGamemodeExecutor extends SubCommandExecutor {

    private static final String FORMAT_CAN = "Players in %s mode may use stepsize.";
    private static final String FORMAT_CANNOT = "Players in %s mode may NOT use stepsize.";

    public StepsizeGamemodeExecutor(VarLightSubCommand command) {
        super(command);
    }

    public int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        for (GameMode gameMode : Arrays.asList(GameMode.SURVIVAL, GameMode.CREATIVE, GameMode.ADVENTURE)) {
            if (plugin.getConfiguration().isAllowedStepsizeGamemode(gameMode)) {
                CommandResult.info(command, context.getSource(), String.format(FORMAT_CAN, gameMode.name()), ChatColor.GREEN);
            } else {
                CommandResult.info(command, context.getSource(), String.format(FORMAT_CANNOT, gameMode.name()), ChatColor.RED);
            }
        }

        return SUCCESS;
    }

    public int executeAllow(CommandContext<CommandSender> context) throws CommandSyntaxException {
        GameMode gameMode = context.getArgument("gamemode", GameMode.class);

        if (gameMode == GameMode.SPECTATOR) {
            CommandResult.info(command, context.getSource(), "Spectators cannot use stepsize.", ChatColor.RED);
            return SUCCESS;
        }

        plugin.getConfiguration().setCanUseStepsize(gameMode, true);

        CommandResult.successBroadcast(command, context.getSource(), String.format("Allowed Players with %s gamemode to use stepsize.", gameMode.name()));

        return SUCCESS;
    }

    public int executeDisallow(CommandContext<CommandSender> context) throws CommandSyntaxException {
        GameMode gameMode = context.getArgument("gamemode", GameMode.class);

        if (gameMode == GameMode.SPECTATOR) {
            CommandResult.info(command, context.getSource(), "Spectators cannot use stepsize.", ChatColor.RED);
            return SUCCESS;
        }

        plugin.getConfiguration().setCanUseStepsize(gameMode, false);

        CommandResult.successBroadcast(command, context.getSource(), String.format("Disallowed Players with %s gamemode to use stepsize.", gameMode.name()));

        return SUCCESS;
    }
}
