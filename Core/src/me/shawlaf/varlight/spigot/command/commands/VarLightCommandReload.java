package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class VarLightCommandReload extends VarLightSubCommand {
    public VarLightCommandReload(VarLightPlugin plugin) {
        super(plugin, "reload");
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.reload";
    }

    @NotNull
    @Override
    public String getSyntax() {
        return "";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Reload the configuration file";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.executes(
                context -> {
                    plugin.reloadConfig();
                    plugin.reload();

                    successBroadcast(this, context.getSource(), "Configuration reloaded!");

                    return SUCCESS;
                }
        );

        return node;
    }
}
