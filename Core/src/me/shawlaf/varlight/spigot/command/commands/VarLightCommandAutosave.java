package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static me.shawlaf.command.result.CommandResult.successBroadcast;

@Deprecated
public class VarLightCommandAutosave extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_NEW_INTERVAL = argument("newInterval", integer());

    public VarLightCommandAutosave(VarLightPlugin varLightPlugin) {
        super(varLightPlugin, "autosave");
    }

    @NotNull
    @Override
    public String getRequiredPermission() {
        return "varlight.admin.save";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Adjust the autosave interval";
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " <new interval>";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {
        return literalArgumentBuilder
                .then(
                        ARG_NEW_INTERVAL.executes(context -> {
                            int newInterval = context.getArgument(ARG_NEW_INTERVAL.getName(), int.class);

                            plugin.getConfiguration().setAutosaveInterval(newInterval);
                            plugin.getAutosaveManager().update(newInterval);

                            if (newInterval > 0) {
                                successBroadcast(this, context.getSource(), String.format("Updated Autosave interval to %d Minutes", newInterval));
                                return VarLightCommand.SUCCESS;
                            } else if (newInterval == 0) {
                                successBroadcast(this, context.getSource(), "Disabled Autosave");
                                return VarLightCommand.SUCCESS;
                            } else {
                                successBroadcast(this, context.getSource(), "Enabled Persist On Save");
                                return VarLightCommand.SUCCESS;
                            }
                        })
                );
    }
}
