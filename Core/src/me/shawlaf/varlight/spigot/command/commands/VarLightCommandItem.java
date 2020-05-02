package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.success;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;
import static me.shawlaf.varlight.spigot.command.commands.arguments.MinecraftTypeArgumentType.minecraftType;

@Deprecated
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
                                failure(this, context.getSource(), String.format("%s cannot be used as the varlight update item", item.getKey().toString()));

                                return FAILURE;
                            }

                            plugin.setUpdateItem(item);
                            success(this, context.getSource(), String.format("Updated the Light update item to %s", item.getKey().toString()));

                            return SUCCESS;
                        })
        );

        return node;
    }
}
