package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class WorldListExecutor extends SubCommandExecutor {

    private final VarLightConfiguration.WorldListType worldListType;

    public WorldListExecutor(VarLightSubCommand command, VarLightConfiguration.WorldListType worldListType) {
        super(command);

        this.worldListType = worldListType;
    }

    int executeList(CommandContext<CommandSender> context) throws CommandSyntaxException {
        List<String> worldList = plugin.getConfiguration().getWorldNames(worldListType);

        info(command, context.getSource(), String.format("There are %d worlds on the %s:", worldList.size(), worldListType.getName()));

        for (String worldName : worldList) {
            context.getSource().sendMessage(String.format(" - %s", worldName));
        }

        return SUCCESS;
    }

    int executeAdd(CommandContext<CommandSender> context) throws CommandSyntaxException {
        World world = context.getArgument("world", World.class);

        if (plugin.getConfiguration().getWorldNames(worldListType).contains(world.getName())) {
            info(command, context.getSource(), String.format("That world is already on the %s.", worldListType.getName()));
            return SUCCESS;
        }

        plugin.getConfiguration().addWorldToList(world, worldListType);
        successBroadcast(command, context.getSource(), String.format("Added World \"%s\" to the %s (changes effective after restart).", world.getName(), worldListType.getName()));

        return SUCCESS;
    }

    int executeRemove(CommandContext<CommandSender> context) throws CommandSyntaxException {
        World world = context.getArgument("world", World.class);

        if (!plugin.getConfiguration().getWorldNames(worldListType).contains(world.getName())) {
            info(command, context.getSource(), String.format("That world is not on the %s.", worldListType.getName()));
            return SUCCESS;
        }

        plugin.getConfiguration().removeWorldFromList(world, worldListType);
        successBroadcast(command, context.getSource(), String.format("Removed World \"%s\" from the %s (changes effective after restart).", world.getName(), worldListType.getName()));

        return SUCCESS;
    }

    int executeClear(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getConfiguration().clearWorldList(worldListType);

        successBroadcast(command, context.getSource(), String.format("Cleared the %s (changes effective after restart).", worldListType.getName()));

        return SUCCESS;
    }
}
