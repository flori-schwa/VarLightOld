package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

public class VarLightCommandAutosave extends VarLightSubCommand {

    public VarLightCommandAutosave(VarLightPlugin varLightPlugin) {
        super(varLightPlugin, "varlight-autosave");
    }

    @Override
    public String getSubCommandName() {
        return "autosave";
    }

    @Override
    public String getRequiredPermission() {
        return "varlight.admin.save";
    }

    @Override
    public String getDescription() {
        return "Adjust the autosave interval";
    }

    @Override
    public String getSyntax() {
        return " <new interval>";
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {
        return literalArgumentBuilder
                .then(
                        RequiredArgumentBuilder.<CommandSender, Integer>argument("newInterval", IntegerArgumentType.integer()).executes(context -> {
                            int newInterval = context.getArgument("newInterval", Integer.class);

                            if (newInterval > 0) {
                                successBroadcast(String.format("Updated Autosave interval to %d Minutes", newInterval), "varlight.admin.save").finish(context.getSource());
                                return 0;
                            } else if (newInterval == 0) {
                                successBroadcast("Disabled Autosave", "varlight.admin.save").finish(context.getSource());
                                return 0;
                            } else {
                                successBroadcast("Enabled Persist On Save", "varlight.admin.save").finish(context.getSource());
                                return 0;
                            }
                        })
                );
    }
}
