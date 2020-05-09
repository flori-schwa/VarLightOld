package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class UpdateCheckExecutor extends SubCommandExecutor {
    public UpdateCheckExecutor(VarLightSubCommand command) {
        super(command);
    }

    public int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        if (plugin.getConfiguration().isCheckUpdateEnabled()) {
            info(command, context.getSource(), "Update checking is enabled.", ChatColor.GREEN);
        } else {
            info(command, context.getSource(), "Update checking is disabled.", ChatColor.RED);
        }

        return SUCCESS;
    }

    public int executeEnable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getConfiguration().setCheckUpdate(true);

        successBroadcast(command, context.getSource(), "Enabled update checking.");

        return SUCCESS;
    }

    public int executeDisable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getConfiguration().setCheckUpdate(false);

        successBroadcast(command, context.getSource(), "Disabled update checking.");

        return SUCCESS;
    }
}
