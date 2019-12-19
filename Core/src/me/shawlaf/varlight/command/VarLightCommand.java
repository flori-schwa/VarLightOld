package me.shawlaf.varlight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.florian.command.brigadier.BrigadierCommand;
import me.florian.command.exception.CommandException;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.commands.VarLightCommandAutosave;
import me.shawlaf.varlight.command.commands.VarLightCommandHelp;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("rawtypes")
public class VarLightCommand extends BrigadierCommand<CommandSender, VarLightPlugin> {

    private static final Class[] SUB_COMMANDS;

    static {
        SUB_COMMANDS = new Class[]{
                // Register sub commands here
                VarLightCommandAutosave.class

        };
    }

    private VarLightSubCommand[] subCommands; // Will be used by help command
    private int counter = 0;

    public VarLightCommand(VarLightPlugin plugin) {
        super(plugin, "varlight", CommandSender.class);
    }

    @Override
    public String getDescription() {
        return "The Varlight root command";
    }

    private void registerSubCommand(Class subCommandClass, LiteralArgumentBuilder<CommandSender> root) {
        VarLightSubCommand subCommand;

        try {
            subCommand = (VarLightSubCommand) subCommandClass.getConstructor(VarLightPlugin.class).newInstance(plugin);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw CommandException.severeException("Failed to register command " + subCommandClass.getSimpleName(), e);
        }

        registerSubCommand(subCommand, root);
    }

    private void registerSubCommand(VarLightSubCommand subCommand, LiteralArgumentBuilder<CommandSender> root) {
        if (subCommands == null) {
            subCommands = new VarLightSubCommand[SUB_COMMANDS.length];
        }

        subCommands[counter++] = subCommand;

        // Constructor registers sub Command as separate command

        LiteralArgumentBuilder<CommandSender> subCommandRoot = LiteralArgumentBuilder.literal(subCommand.getSubCommandName());

        subCommand.buildFrom(subCommandRoot);
        root.then(subCommandRoot);
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> builder) {
        for (Class clazz : SUB_COMMANDS) {
            registerSubCommand(clazz, builder);
        }

        VarLightCommandHelp helpCommand = new VarLightCommandHelp(plugin, this);

        LiteralArgumentBuilder<CommandSender> subCommandRoot = LiteralArgumentBuilder.literal(helpCommand.getSubCommandName());

        helpCommand.buildFrom(subCommandRoot);
        builder.then(subCommandRoot);

        return builder;
    }

    public VarLightSubCommand[] getSubCommands() {
        return subCommands;
    }
}
