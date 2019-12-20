package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightCommand;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;

import java.util.Optional;

public class VarLightCommandHelp extends VarLightSubCommand {

    private VarLightCommand rootCommand;

    public VarLightCommandHelp(VarLightPlugin varLightPlugin, VarLightCommand rootCommand) {
        super(varLightPlugin, "varlight-help", true);

        this.rootCommand = rootCommand;
        build();
    }

    @Override
    public String getSubCommandName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays all VarLight sub commands";
    }

    @Override
    public String getSyntax() {
        return " [command|page]";
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {

        for (VarLightSubCommand subCommand : rootCommand.getSubCommands()) {
            literalArgumentBuilder.then(
                    LiteralArgumentBuilder.<CommandSender>literal(subCommand.getSubCommandName())
                            .requires(sender -> sender.hasPermission(Optional.ofNullable(subCommand.getRequiredPermission()).orElse("")))
                            .executes(context -> {
                                context.getSource().sendMessage(subCommand.getCommandHelp());
                                return 0;
                            })
            );
        }

        literalArgumentBuilder.then(
                RequiredArgumentBuilder.<CommandSender, Integer>argument("page", IntegerArgumentType.integer())
                        .executes(context -> {
                            int page = Math.max(1, context.getArgument("page", Integer.class));

                            showHelp(context.getSource(), page);
                            return 0;
                        })
        );

        literalArgumentBuilder.executes(
                context -> {
                    showHelp(context.getSource());
                    return 0;
                }
        );

        return literalArgumentBuilder;
    }

    private String getFullHelpRaw(CommandSender commandSender) {
        StringBuilder builder = new StringBuilder();

        for (VarLightSubCommand subCommand : rootCommand.getSubCommands()) {
            String help = subCommand.getCommandHelp();

            if (help != null && commandSender.hasPermission(Optional.ofNullable(subCommand.getRequiredPermission()).orElse(""))) {
                builder.append(help).append('\n');
            }
        }

        return builder.toString().trim();
    }

    public void showHelp(CommandSender sender) {
        showHelp(sender, 1);
    }

    public void showHelp(CommandSender sender, int page) {
        ChatPaginator.ChatPage chatPage = ChatPaginator.paginate(getFullHelpRaw(sender), page);

        sender.sendMessage(ChatColor.GRAY + "-----------------------------------");
        sender.sendMessage(String.format("%sVarLight command help: %s[Page %d / %d]", ChatColor.GOLD, ChatColor.RESET, chatPage.getPageNumber(), chatPage.getTotalPages()));

        for (String line : chatPage.getLines()) {
            sender.sendMessage(line);
        }
    }
}
