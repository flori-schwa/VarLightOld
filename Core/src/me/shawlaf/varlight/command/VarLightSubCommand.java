package me.shawlaf.varlight.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.florian.command.ICommandAccess;
import me.shawlaf.varlight.VarLightPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public abstract class VarLightSubCommand implements ICommandAccess<VarLightPlugin> {

    protected final VarLightPlugin plugin;
    private final String name;

    public VarLightSubCommand(VarLightPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public abstract @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node);

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
}
