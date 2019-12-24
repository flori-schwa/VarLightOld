package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VarLightCommandPermission extends VarLightSubCommand {
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
        return node;
    }
}
