package me.shawlaf.varlight.spigot.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import me.shawlaf.command.ICommandAccess;
import me.shawlaf.command.brigadier.argument.EnumArgumentType;
import me.shawlaf.command.brigadier.argument.PlayerArgumentType;
import me.shawlaf.command.brigadier.argument.PositionArgumentType;
import me.shawlaf.command.brigadier.argument.WorldArgumentType;
import me.shawlaf.command.brigadier.datatypes.ICoordinates;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.commands.arguments.CollectionArgumentType;
import me.shawlaf.varlight.spigot.command.commands.arguments.MinecraftTypeArgumentType;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class VarLightSubCommand implements ICommandAccess<VarLightPlugin> {

    protected final VarLightPlugin plugin;
    protected final VarLightCommand rootCommand;
    private final String name;

    public VarLightSubCommand(VarLightCommand rootCommand, String name) {
        this.rootCommand = rootCommand;
        this.plugin = rootCommand.getPlugin();
        this.name = name;
    }

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

    public abstract @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node);

    public boolean meetsRequirement(CommandSender commandSender) {
        String required = getRequiredPermission();

        if (required.isEmpty()) {
            return true;
        }

        return commandSender.hasPermission(required);
    }

    // region Util

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

    public CommandNode<CommandSender> getNode() {
        return rootCommand.getCommandDispatcher().getRoot().getChildren().iterator().next().getChild(getName());
    }

    @Override
    public @NotNull String getUsageString() {
        return getUsageString(Bukkit.getConsoleSender());
    }

    @Override
    public @NotNull String getUsageString(CommandSender commandSender) {
        return "/varlight " + rootCommand.getCommandDispatcher().getSmartUsage(rootCommand.getCommandDispatcher().getRoot().getChild(rootCommand.getName()), commandSender).get(getNode());
    }

    protected RequiredArgumentBuilder<CommandSender, Material> minecraftTypeArgument(String name, MaterialType materialType) {
        return RequiredArgumentBuilder.argument(name, MinecraftTypeArgumentType.minecraftType(plugin, materialType));
    }

    protected <E extends Enum<E>> RequiredArgumentBuilder<CommandSender, E> enumArgument(String name, Class<E> enumType) {
        return RequiredArgumentBuilder.argument(name, EnumArgumentType.enumArgument(enumType));
    }

    // endregion
}
