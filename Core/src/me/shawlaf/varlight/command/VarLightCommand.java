package me.shawlaf.varlight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.command.brigadier.BrigadierCommand;
import me.shawlaf.command.exception.CommandException;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.commands.*;
import me.shawlaf.varlight.command.commands.world.VarLightCommandBlacklist;
import me.shawlaf.varlight.command.commands.world.VarLightCommandWhitelist;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("rawtypes")
public class VarLightCommand extends BrigadierCommand<CommandSender, VarLightPlugin> {

    /*
        // TODO:
            - Hide commands without permission from brigadier auto completion
            - Give proper no permission messages instead of "Incorrect argument..."
     */

    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;

    private static final Class[] SUB_COMMANDS;

    static {
        SUB_COMMANDS = new Class[]{
                // Register sub commands here
                VarLightCommandAutosave.class,
                VarLightCommandDebug.class,
                VarLightCommandMigrate.class,
                VarLightCommandPermission.class,
                VarLightCommandReload.class,
                VarLightCommandSave.class,
                VarLightCommandStepSize.class,
                VarLightCommandSuggest.class,
                VarLightCommandUpdate.class,
                VarLightCommandWhitelist.class,
                VarLightCommandBlacklist.class,
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

        LiteralArgumentBuilder<CommandSender> subCommandRoot = LiteralArgumentBuilder.literal(subCommand.getName());

        subCommandRoot.requires(sender -> {
            String required = subCommand.getRequiredPermission();

            if (required.isEmpty()) {
                return true;
            }

            return sender.hasPermission(required);
        });

        subCommand.build(subCommandRoot);

        root.then(subCommandRoot);
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> builder) {
        for (Class clazz : SUB_COMMANDS) {
            registerSubCommand(clazz, builder);
        }

        VarLightCommandHelp helpCommand = new VarLightCommandHelp(plugin, this);
        LiteralArgumentBuilder<CommandSender> subCommandRoot = LiteralArgumentBuilder.literal(helpCommand.getName());
        helpCommand.build(subCommandRoot);

        builder.then(subCommandRoot);

        return builder;
    }

    @Override
    public @Nullable String getRequiredPermission() {
        return "";
    }

    public VarLightSubCommand[] getSubCommands() {
        return subCommands;
    }
}
