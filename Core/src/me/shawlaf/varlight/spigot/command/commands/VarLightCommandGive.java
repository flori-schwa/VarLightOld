package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandGive extends VarLightSubCommand {

    private static RequiredArgumentBuilder<CommandSender, Player> ARG_TARGET = playerArgument("target");
    private static RequiredArgumentBuilder<CommandSender, Integer> ARG_LIGHT_LEVEL = integerArgument("light level", 1, 15);
    private static RequiredArgumentBuilder<CommandSender, Integer> ARG_AMOUNT = integerArgument("amount", 1);

    public VarLightCommandGive(VarLightCommand command) {
        super(command, "give");
    }

    @Override
    public @NotNull String getDescription() {
        return "Give yourself or someone else some glowing blocks.";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.give";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(ARG_TARGET.then(
                minecraftTypeArgument("item", MaterialType.BLOCK)
                        .then(ARG_LIGHT_LEVEL
                                .executes(context -> give(context, 1))
                                .then(ARG_AMOUNT.executes(context -> give(context, context.getArgument(ARG_AMOUNT.getName(), int.class))))
                        )
                )
        );

        return node;
    }

    private int give(CommandContext<CommandSender> context, int amount) {
        Player target = context.getArgument(ARG_TARGET.getName(), Player.class);
        Material type = context.getArgument("item", Material.class);
        int lightLevel = context.getArgument(ARG_LIGHT_LEVEL.getName(), int.class);

        int given = 0;

        ItemStack base = new ItemStack(type);

        while (given < amount) {
            ItemStack toGive = plugin.getNmsAdapter().makeGlowingStack(base, lightLevel);
            toGive.setAmount(Math.min(64, amount - given));

            if (toGive.getAmount() > 0) {
                target.getInventory().addItem(toGive);
                given += toGive.getAmount();
            }
        }

        successBroadcast(this, context.getSource(),
                String.format("%s gave %d \"Glowing %s\" to %s",
                        context.getSource().getName(),
                        amount,
                        plugin.getNmsAdapter().getLocalizedBlockName(type),
                        target.getName())
        );

        return SUCCESS;
    }
}
