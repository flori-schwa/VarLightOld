package me.shawlaf.varlight.spigot.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.command.ICommandAccess;
import me.shawlaf.command.brigadier.argument.PlayerArgumentType;
import me.shawlaf.command.brigadier.argument.PositionArgumentType;
import me.shawlaf.command.brigadier.argument.WorldArgumentType;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.commands.arguments.CollectionArgumentType;
import me.shawlaf.varlight.spigot.command.commands.arguments.MinecraftTypeArgumentType;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class VarLightSubCommand implements ICommandAccess<VarLightPlugin> {

    protected final VarLightPlugin plugin;
    private final String name;

    public VarLightSubCommand(VarLightPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public abstract @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node);

    public boolean meetsRequirement(CommandSender commandSender) {
        String required = getRequiredPermission();

        if (required.isEmpty()) {
            return true;
        }

        return commandSender.hasPermission(required);
    }

    @Override
    public final @NotNull VarLightPlugin getPlugin() {
        return plugin;
    }

    @Override
    public final @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getSyntax() {
        return "";
    }

    @Override
    public @NotNull String getDescription() {
        return "";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "";
    }

    @Override
    public final @NotNull String[] getAliases() {
        return new String[0];
    }

    @Override
    public @NotNull String getUsageString() {
        if (getSyntax().isEmpty() || getDescription().isEmpty()) {
            return "";
        }

        return ChatColor.GOLD + "/varlight " + getName() + getSyntax() + ": " + ChatColor.RESET + getDescription();
    }

    // region Util

    protected static LiteralArgumentBuilder<CommandSender> literalArgument(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    protected static RequiredArgumentBuilder<CommandSender, Integer> integerArgument(String name) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer());
    }

    protected static RequiredArgumentBuilder<CommandSender, Integer> integerArgument(String name, int min) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer(min));
    }

    protected static RequiredArgumentBuilder<CommandSender, Integer> integerArgument(String name, int min, int max) {
        return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer(min, max));
    }

    protected static RequiredArgumentBuilder<CommandSender, Boolean> boolArgument(String name) {
        return RequiredArgumentBuilder.argument(name, BoolArgumentType.bool());
    }

    protected static RequiredArgumentBuilder<CommandSender, ICoordinates> positionArgument(String name) {
        return RequiredArgumentBuilder.argument(name, PositionArgumentType.position());
    }

    protected static <T> RequiredArgumentBuilder<CommandSender, Collection<T>> collectionArgument(String name, ArgumentType<T> argument) {
        return RequiredArgumentBuilder.argument(name, CollectionArgumentType.collection(argument));
    }

    protected static RequiredArgumentBuilder<CommandSender, World> worldArgument(String name) {
        return RequiredArgumentBuilder.argument(name, WorldArgumentType.world());
    }

    protected static RequiredArgumentBuilder<CommandSender, Player> playerArgument(String name) {
        return RequiredArgumentBuilder.argument(name, PlayerArgumentType.player());
    }

    protected RequiredArgumentBuilder<CommandSender, Material> minecraftTypeArgument(String name, MaterialType materialType) {
        return RequiredArgumentBuilder.argument(name, MinecraftTypeArgumentType.minecraftType(plugin, materialType));
    }

    // endregion
}
