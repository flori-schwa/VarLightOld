package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import me.shawlaf.varlight.nms.MaterialType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.success;
import static me.shawlaf.varlight.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.command.VarLightCommand.SUCCESS;
import static me.shawlaf.varlight.command.commands.arguments.MinecraftTypeArgumentType.minecraftType;

public class VarLightCommandItem extends VarLightSubCommand {
    public VarLightCommandItem(VarLightPlugin plugin) {
        super(plugin, "item");
    }

    @Override
    public @NotNull String getDescription() {
        return "Updates the item required to use the plugin features";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.item";
    }

    @Override
    public @NotNull String getSyntax() {
        return " <type>";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(
                RequiredArgumentBuilder.<CommandSender, Material>argument("item", minecraftType(plugin, MaterialType.ITEM))
                        .executes(context -> {
                            Material item = context.getArgument("item", Material.class);

                            if (plugin.getNmsAdapter().isIllegalLightUpdateItem(item)) {
                                failure(this, context.getSource(), String.format("%s cannot be used as the varlight update item", plugin.getNmsAdapter().materialToKey(item)));

                                return FAILURE;
                            }

                            plugin.setUpdateItem(item);
                            success(this, context.getSource(), String.format("Updated the Light update item to %s", plugin.getNmsAdapter().materialToKey(item)));

                            return SUCCESS;
                        })
        );

        return node;
    }
}
