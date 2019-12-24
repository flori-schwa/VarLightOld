package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.command.VarLightCommand.SUCCESS;

public class VarLightCommandPermission extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, String> ARG_NEW_NODE = argument("node", word());

    public VarLightCommandPermission(VarLightPlugin plugin) {
        super(plugin, "perm");
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.perm";
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " get/set/unset [new permission]";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Gets/Sets/Unsets the permission node that is required to use the plugin's functionality";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("get").executes(this::get)
        );

        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("set")
                        .then(ARG_NEW_NODE.executes(this::set))
        );

        node.then(
                LiteralArgumentBuilder.<CommandSender>literal("unset").executes(this::unset)
        );

        return node;
    }

    private int get(CommandContext<CommandSender> context) {
        info(this, context.getSource(), String.format("The current required permission node is \"%s\".", plugin.getConfiguration().getRequiredPermissionNode()));

        return SUCCESS;
    }

    private int set(CommandContext<CommandSender> context) {
        String newNode = context.getArgument(ARG_NEW_NODE.getName(), String.class);
        String oldNode = plugin.getConfiguration().getRequiredPermissionNode();

        plugin.getConfiguration().setRequiredPermissionNode(newNode);

        successBroadcast(this, context.getSource(), String.format("The required permission node has been updated from \"%s\" to \"%s\".", oldNode, newNode));

        return SUCCESS;
    }

    private int unset(CommandContext<CommandSender> context) {
        plugin.getConfiguration().setRequiredPermissionNode("");

        successBroadcast(this, context.getSource(), "The required permission node has been un-set.");

        return SUCCESS;
    }
}
