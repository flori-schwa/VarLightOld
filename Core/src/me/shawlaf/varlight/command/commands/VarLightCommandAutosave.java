package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.florian.command.result.CommandResult.successBroadcast;

public class VarLightCommandAutosave extends VarLightSubCommand {

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
        return "<new interval>";
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {
        return literalArgumentBuilder
                .then(
                        RequiredArgumentBuilder.<CommandSender, Integer>argument("newInterval", IntegerArgumentType.integer()).executes(context -> {
                            int newInterval = context.getArgument("newInterval", int.class);

                            if (newInterval > 0) {
                                successBroadcast(this, context.getSource(), String.format("Updated Autosave interval to %d Minutes", newInterval));
                                return 0;
                            } else if (newInterval == 0) {
                                successBroadcast(this, context.getSource(), "Disabled Autosave");
                                return 0;
                            } else {
                                successBroadcast(this, context.getSource(), "Enabled Persist On Save");
                                return 0;
                            }
                        })
                );
    }
}
