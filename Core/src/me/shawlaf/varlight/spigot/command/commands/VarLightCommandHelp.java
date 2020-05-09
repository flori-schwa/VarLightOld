package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Stream;

import static me.shawlaf.command.result.CommandResult.info;

public class VarLightCommandHelp extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_PAGE = integerArgument("page", 1);

    public VarLightCommandHelp(VarLightCommand rootCommand) {
        super(rootCommand, "help");
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Displays help on all VarLight Sub commands.";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {
        Stream.concat(Arrays.stream(rootCommand.getSubCommands()), Stream.of(this)).forEach(subCommand -> {
            literalArgumentBuilder.then(
                    literalArgument(subCommand.getName())
                            .requires(subCommand::meetsRequirement)
                            .executes(context -> {
                                context.getSource().sendMessage(ChatColor.GRAY + "-----------------------------------");
                                info(this, context.getSource(), "Usage of /varlight " + subCommand.getName() + ":", ChatColor.GOLD);
                                context.getSource().sendMessage("");
                                context.getSource().sendMessage(ChatColor.GOLD + "Syntax: " + subCommand.getUsageString(context.getSource()));
                                context.getSource().sendMessage("");
                                context.getSource().sendMessage(ChatColor.GOLD + "Description: " + subCommand.getDescription());

                                return VarLightCommand.SUCCESS;
                            })
            );
        });

        literalArgumentBuilder.then(
                ARG_PAGE.executes(context -> {
                    int page = context.getArgument(ARG_PAGE.getName(), int.class);

                    showHelp(context.getSource(), page);
                    return VarLightCommand.SUCCESS;
                })
        );

        literalArgumentBuilder.executes(
                context -> {
                    showHelp(context.getSource());
                    return VarLightCommand.SUCCESS;
                }
        );

        return literalArgumentBuilder;
    }

    private String getFullHelpRaw(CommandSender commandSender) {
        StringBuilder builder = new StringBuilder();

        for (VarLightSubCommand subCommand : rootCommand.getSubCommands()) {
            String help = subCommand.getUsageString(commandSender);

            if (!help.isEmpty() && subCommand.meetsRequirement(commandSender)) {
                builder.append(help).append('\n');
            }
        }

        return builder.toString().trim();
    }

    public void showHelp(CommandSender sender) {
        showHelp(sender, 1);
    }

    public void showHelp(CommandSender sender, int page) {
        ChatPaginator.ChatPage chatPage = ChatPaginator.paginate(getFullHelpRaw(sender), page, 100, 10);

        sender.sendMessage(ChatColor.GRAY + "-----------------------------------");
        sender.sendMessage(String.format("%sVarLight command help: %s[Page %d / %d]", ChatColor.GOLD, ChatColor.RESET, chatPage.getPageNumber(), chatPage.getTotalPages()));

        for (String line : chatPage.getLines()) {
            sender.sendMessage(line);
        }

        sender.sendMessage("");
        sender.sendMessage(String.format("%sRun /varlight help <sub command> to get more info about a specific command.", ChatColor.GOLD));
    }
}
