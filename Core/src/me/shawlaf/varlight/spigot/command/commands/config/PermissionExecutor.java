package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class PermissionExecutor extends SubCommandExecutor {

    public PermissionExecutor(VarLightSubCommand command) {
        super(command);
    }

    int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        return SUCCESS;
    }

    int executeSet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        return SUCCESS;
    }
}
