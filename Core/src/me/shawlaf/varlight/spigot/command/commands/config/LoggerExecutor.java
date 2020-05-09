package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class LoggerExecutor extends SubCommandExecutor {
    public LoggerExecutor(VarLightSubCommand command) {
        super(command);
    }

    public int executeVerboseGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        if (plugin.getConfiguration().isLogVerbose()) {
            info(command, context.getSource(), "Verbose logging is enabled.", ChatColor.GREEN);
        } else {
            info(command, context.getSource(), "Verbose logging is disabled.", ChatColor.RED);
        }

        return SUCCESS;
    }

    public int executeVerboseEnable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getConfiguration().setLogVerbose(true);

        successBroadcast(command, context.getSource(), "Enabled Verbose logging.");

        return SUCCESS;
    }

    public int executeVerboseDisable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getConfiguration().setLogVerbose(false);

        successBroadcast(command, context.getSource(), "Disabled Verbose logging.");

        return SUCCESS;
    }

    public int executeDebugGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        if (plugin.getConfiguration().isLogDebug()) {
            info(command, context.getSource(), "Debug logging is enabled.", ChatColor.GREEN);
        } else {
            info(command, context.getSource(), "Debug logging is disabled.", ChatColor.RED);
        }

        return SUCCESS;
    }

    public int executeDebugEnable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getConfiguration().setLogDebug(true);

        successBroadcast(command, context.getSource(), "Enabled Debug logging.");

        return SUCCESS;
    }

    public int executeDebugDisable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getConfiguration().setLogDebug(false);

        successBroadcast(command, context.getSource(), "Disabled Debug logging.");

        return SUCCESS;
    }
}
