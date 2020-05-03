package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.command.result.CommandResult;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class AutosaveExecutor extends SubCommandExecutor {

    public AutosaveExecutor(VarLightSubCommand command) {
        super(command);
    }

    int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {
        int autosaveInterval = plugin.getConfiguration().getAutosaveInterval();

        if (autosaveInterval > 0) {
            CommandResult.info(command, context.getSource(), String.format("Light sources are automatically saved every %d Minutes", autosaveInterval));
        } else if (autosaveInterval < 0) {
            CommandResult.info(command, context.getSource(), "Light sources are automatically saved when the world is saved");
        } else {
            CommandResult.info(command, context.getSource(), "Light sources are not automatically saved.");
        }

        return SUCCESS;
    }

    int executeSet(CommandContext<CommandSender> context) throws CommandSyntaxException {
        int newInterval = context.getArgument("newInterval", int.class);

        plugin.getConfiguration().setAutosaveInterval(newInterval);
        plugin.getAutosaveManager().update(newInterval);

        if (newInterval > 0) {
            successBroadcast(command, context.getSource(), String.format("Updated Autosave interval to %d Minutes", newInterval));
        } else if (newInterval == 0) {
            successBroadcast(command, context.getSource(), "Disabled Autosave");
        } else {
            successBroadcast(command, context.getSource(), "Enabled Persist On Save");
        }

        return SUCCESS;
    }
}
