package me.shawlaf.varlight.spigot.command;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import me.shawlaf.command.ArgumentIterator;
import me.shawlaf.command.CommandSuggestions;
import me.shawlaf.command.brigadier.BrigadierCommand;
import me.shawlaf.command.exception.CommandException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.commands.*;
import me.shawlaf.varlight.spigot.command.commands.world.VarLightCommandBlacklist;
import me.shawlaf.varlight.spigot.command.commands.world.VarLightCommandWhitelist;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("rawtypes")
public class VarLightCommand extends BrigadierCommand<CommandSender, VarLightPlugin> {

    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;

    private static final Class[] SUB_COMMANDS = new Class[]{
            // Register sub commands here
            VarLightCommandAutosave.class,
            VarLightCommandDebug.class,
            VarLightCommandFill.class,
            VarLightCommandItem.class,
            VarLightCommandPermission.class,
            VarLightCommandReload.class,
            VarLightCommandSave.class,
            VarLightCommandStepSize.class,
            VarLightCommandUpdate.class,
            VarLightCommandGive.class,

            VarLightCommandWhitelist.class,
            VarLightCommandBlacklist.class,
    };

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

        subCommandRoot.requires(subCommand::meetsRequirement);

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

    @Override
    public void tabComplete(CommandSuggestions suggestions) {
        try {
            String fullInput = getFullInput(new ArgumentIterator(suggestions.getArguments()));

            CommandSender source = suggestions.getCommandSender();
            ParseResults<CommandSender> parseResults = commandDispatcher.parse(fullInput, source);

            Suggestions completionSuggestions = commandDispatcher.getCompletionSuggestions(parseResults).get();

            for (Suggestion suggestion : completionSuggestions.getList()) {
                String input = suggestion.apply(fullInput);

                CommandNode<CommandSender> node = commandDispatcher.findNode(Arrays.asList(input.split(" ")));

                if (node == null || node.canUse(source)) {
                    suggestions.add(suggestion.getText());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
