package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.varlight.spigot.VarLightConfiguration.WorldListType.BLACKLIST;
import static me.shawlaf.varlight.spigot.VarLightConfiguration.WorldListType.WHITELIST;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandConfig extends VarLightSubCommand {

    public VarLightCommandConfig(VarLightPlugin plugin) {
        super(plugin, "config");
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.then(
                literalArgument("autosave")
                        .then(literalArgument("get").executes(this::runGetAutosaveInterval))
                        .then(
                                literalArgument("set")
                                        .then(
                                                integerArgument("newInterval")
                                                        .executes(this::runSetAutosaveInterval)
                                        )
                        )
                        .then(literalArgument("reload").executes(this::runReloadAutosaveInterval))
        );

        node.then(
                literalArgument("item")
                        .then(literalArgument("get").executes(this::runGetLUI))
                        .then(
                                literalArgument("set")
                                        .then(
                                                minecraftTypeArgument("item", MaterialType.ITEM)
                                                        .executes(this::runSetLUI)
                                        )
                        )
                        .then(literalArgument("reload").executes(this::runReloadLUI))
        );

        node.then(
                literalArgument("permission")
                        .then(literalArgument("get").executes(this::runGetPermissionCheck))
                        .then(
                                literalArgument("set")
                                        .then(boolArgument("value"))
                                        .executes(this::runSetPermissionCheck)
                        )
                        .then(literalArgument("reload")).executes(this::runReloadPermissionCheck)
        );

        node.then(
                literalArgument("whitelist")
                        .then(literalArgument("list").executes(c -> runListWorldList(c, WHITELIST)))
                        .then(
                                literalArgument("add")
                                        .then(
                                                worldArgument("world").executes(c -> runAddToWorldList(c, WHITELIST))
                                        )
                        )
                        .then(
                                literalArgument("remove")
                                        .then(worldArgument("world").executes(c -> runRemoveFromWorldList(c, WHITELIST)))
                        )
                        .then(literalArgument("clear")).executes(c -> runClearWorldList(c, WHITELIST))
                        .then(literalArgument("reload").executes(c -> runReloadWorldList(c, WHITELIST)))
        );

        node.then(
                literalArgument("blacklist")
                        .then(literalArgument("list").executes(c -> runListWorldList(c, BLACKLIST)))
                        .then(
                                literalArgument("add")
                                        .then(
                                                worldArgument("world").executes(c -> runAddToWorldList(c, BLACKLIST))
                                        )
                        )
                        .then(
                                literalArgument("remove")
                                        .then(worldArgument("world").executes(c -> runRemoveFromWorldList(c, BLACKLIST)))
                        )
                        .then(literalArgument("clear")).executes(c -> runClearWorldList(c, BLACKLIST))
                        .then(literalArgument("reload").executes(c -> runReloadWorldList(c, BLACKLIST)))
        );

        /* TODO
            - NLS
            - stepsize
            - reclaim
            - debug log
            - update check
         */

        return node;
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.config";
    }

    // region Autosave Command Implementations

    private int runGetAutosaveInterval(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runGetAutosaveInterval");

        return SUCCESS;
    }

    private int runSetAutosaveInterval(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runSetAutosaveInterval");

        return SUCCESS;
    }

    private int runReloadAutosaveInterval(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runReloadAutosaveInterval");

        return SUCCESS;
    }

    // endregion

    // region Light update Item Command Implementations

    private int runGetLUI(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runGetLUI");

        return SUCCESS;
    }

    private int runSetLUI(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runSetLUI");

        return SUCCESS;
    }

    private int runReloadLUI(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runReloadLUI");

        return SUCCESS;
    }

    // endregion

    // region Permission Check Command Implementations

    private int runGetPermissionCheck(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runGetPermissionCheck");

        return SUCCESS;
    }

    private int runSetPermissionCheck(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runSetPermissionCheck");

        return SUCCESS;
    }

    private int runReloadPermissionCheck(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runReloadPermissionCheck");

        return SUCCESS;
    }

    // endregion

    // region World List Management Command Implementations

    private int runListWorldList(CommandContext<CommandSender> context, VarLightConfiguration.WorldListType listType) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runListWorldList");

        return SUCCESS;
    }

    private int runAddToWorldList(CommandContext<CommandSender> context, VarLightConfiguration.WorldListType listType) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runAddToWorldList");

        return SUCCESS;
    }

    private int runRemoveFromWorldList(CommandContext<CommandSender> context, VarLightConfiguration.WorldListType listType) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runRemoveFromWorldList");

        return SUCCESS;
    }

    private int runClearWorldList(CommandContext<CommandSender> context, VarLightConfiguration.WorldListType listType) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runClearWorldList");

        return SUCCESS;
    }

    private int runReloadWorldList(CommandContext<CommandSender> context, VarLightConfiguration.WorldListType listType) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runReloadWorldList");

        return SUCCESS;
    }

    // endregion

}
