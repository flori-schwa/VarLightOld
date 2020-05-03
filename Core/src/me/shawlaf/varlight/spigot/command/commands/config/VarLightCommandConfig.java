package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.varlight.spigot.VarLightConfiguration.WorldListType.BLACKLIST;
import static me.shawlaf.varlight.spigot.VarLightConfiguration.WorldListType.WHITELIST;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandConfig extends VarLightSubCommand {

    private final AutosaveExecutor autosaveExecutor;
    private final ItemExecutor itemExecutor;
    private final PermissionExecutor permissionExecutor;
    private final WorldListExecutor whitelistExecutor;
    private final WorldListExecutor blacklistExecutor;

    public VarLightCommandConfig(VarLightCommand command) {
        super(command, "config");

        this.autosaveExecutor = new AutosaveExecutor(this);
        this.itemExecutor = new ItemExecutor(this);
        this.permissionExecutor = new PermissionExecutor(this);
        this.whitelistExecutor = new WorldListExecutor(this, WHITELIST);
        this.blacklistExecutor = new WorldListExecutor(this, BLACKLIST);
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.then(literalArgument("reload").executes(this::runReload));

        node.then(
                literalArgument("autosave")
                        .then(literalArgument("get").executes(autosaveExecutor::executeGet))
                        .then(
                                literalArgument("set")
                                        .then(
                                                integerArgument("newInterval")
                                                        .executes(autosaveExecutor::executeSet)
                                        )
                        )
        );

        node.then(
                literalArgument("item")
                        .then(literalArgument("get").executes(itemExecutor::executeGet))
                        .then(
                                literalArgument("set")
                                        .then(
                                                minecraftTypeArgument("item", MaterialType.ITEM)
                                                        .executes(itemExecutor::executeSet)
                                        )
                        )
        );

        node.then(
                literalArgument("permission")
                        .then(literalArgument("get").executes(permissionExecutor::executeGet))
                        .then(
                                literalArgument("set")
                                        .then(boolArgument("value"))
                                        .executes(permissionExecutor::executeSet)
                        )
        );

        node.then(
                literalArgument("whitelist")
                        .then(literalArgument("list").executes(whitelistExecutor::executeList))
                        .then(
                                literalArgument("add")
                                        .then(
                                                worldArgument("world").executes(whitelistExecutor::executeAdd)
                                        )
                        )
                        .then(
                                literalArgument("remove")
                                        .then(worldArgument("world").executes(whitelistExecutor::executeRemove))
                        )
                        .then(literalArgument("clear")).executes(whitelistExecutor::executeClear)
        );

        node.then(
                literalArgument("blacklist")
                        .then(literalArgument("list").executes(blacklistExecutor::executeList))
                        .then(
                                literalArgument("add")
                                        .then(
                                                worldArgument("world").executes(blacklistExecutor::executeAdd)
                                        )
                        )
                        .then(
                                literalArgument("remove")
                                        .then(worldArgument("world").executes(blacklistExecutor::executeRemove))
                        )
                        .then(literalArgument("clear")).executes(blacklistExecutor::executeClear)
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

    private int runReload(CommandContext<CommandSender> context) throws CommandSyntaxException {

        context.getSource().sendMessage("TODO implement runReload");

        return SUCCESS;
    }
}
