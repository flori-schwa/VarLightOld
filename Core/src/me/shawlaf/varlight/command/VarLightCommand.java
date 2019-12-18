package me.shawlaf.varlight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.florian.command.brigadier.BrigadierCommand;
import me.florian.command.exception.CommandException;
import me.shawlaf.varlight.VarLightPlugin;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("rawtypes")
public class VarLightCommand extends BrigadierCommand<CommandSender, VarLightPlugin> {

    private static final Class[] SUB_COMMANDS;

    static {
        SUB_COMMANDS = new Class[] {
                // Register sub commands here

        };
    }

    private VarLightSubCommand[] subCommands = new VarLightSubCommand[SUB_COMMANDS.length]; // Will be used by help command
    private int counter = 0;

    public VarLightCommand(VarLightPlugin plugin) {
        super(plugin, "varlight", CommandSender.class);
    }

    @Override
    public String getDescription() {
        return "The Varlight root command";
    }

    private LiteralArgumentBuilder<CommandSender> registerSubCommand(Class subCommandClass, LiteralArgumentBuilder<CommandSender> root) {
        VarLightSubCommand subCommand;

        try {
            subCommand = (VarLightSubCommand) subCommandClass.getConstructor(VarLightPlugin.class).newInstance(plugin);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw CommandException.severeException("Failed to register command " + subCommandClass.getSimpleName(), e);
        }

        subCommands[counter++] = subCommand;

        // Constructor registers sub Command as separate command

        return subCommand.buildFrom(root);
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> builder) {
        for (Class clazz : SUB_COMMANDS) {
            builder = registerSubCommand(clazz, builder);
        }

        return builder;
    }
}
