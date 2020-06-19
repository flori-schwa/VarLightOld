package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VarLightCommandPrompt extends VarLightSubCommand {
    public VarLightCommandPrompt(VarLightCommand rootCommand) {
        super(rootCommand, "prompt");
    }

    @Override
    public @NotNull String getDescription() {
        return "Used to confirm or cancel prompts";
    }

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.then(
                literalArgument("confirm").executes(c -> {
                    plugin.getChatPromptManager().confirmPrompt(c.getSource());

                    return VarLightCommand.SUCCESS;
                })
        );

        node.then(
                literalArgument("cancel").executes(c -> {
                    plugin.getChatPromptManager().cancelPrompt(c.getSource());

                    return VarLightCommand.SUCCESS;
                })
        );

        return node;
    }
}
