package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class WorldListExecutor extends SubCommandExecutor {

    private final VarLightConfiguration.WorldListType worldListType;

    public WorldListExecutor(VarLightSubCommand command, VarLightConfiguration.WorldListType worldListType) {
        super(command);

        this.worldListType = worldListType;
    }

    int executeList(CommandContext<CommandSender> context) throws CommandSyntaxException {

        return SUCCESS;
    }

    int executeAdd(CommandContext<CommandSender> context) throws CommandSyntaxException {

        return SUCCESS;
    }

    int executeRemove(CommandContext<CommandSender> context) throws CommandSyntaxException {

        return SUCCESS;
    }

    int executeClear(CommandContext<CommandSender> context) throws CommandSyntaxException {

        return SUCCESS;
    }
}
