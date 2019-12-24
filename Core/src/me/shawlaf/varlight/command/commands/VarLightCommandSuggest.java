package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static me.shawlaf.varlight.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.command.VarLightCommand.SUCCESS;

public class VarLightCommandSuggest extends VarLightSubCommand {
    public VarLightCommandSuggest(VarLightPlugin plugin) {
        super(plugin, "suggest");
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        node.then(
                RequiredArgumentBuilder.<CommandSender, String>argument("cmd", greedyString())
                        .executes(
                                context -> {
                                    if (!(context.getSource() instanceof Player)) {
                                        return FAILURE;
                                    }

                                    plugin.getNmsAdapter().suggestCommand((Player) context.getSource(),
                                            context.getArgument("cmd", String.class));

                                    return SUCCESS;
                                }
                        )
        );

        return node;
    }
}
