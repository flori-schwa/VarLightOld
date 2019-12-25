package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightCommand;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class VarLightCommandHelp extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_PAGE = argument("page", integer(1));

    private VarLightCommand rootCommand;

    public VarLightCommandHelp(VarLightPlugin varLightPlugin, VarLightCommand rootCommand) {
        super(varLightPlugin, "help");

        this.rootCommand = rootCommand;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Displays all VarLight sub commands";
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " [command|page]";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {

        for (VarLightSubCommand subCommand : rootCommand.getSubCommands()) {
            if (subCommand.getUsageString().isEmpty()) {
                continue;
            }

            literalArgumentBuilder.then(
                    LiteralArgumentBuilder.<CommandSender>literal(subCommand.getName())
                            .requires(subCommand::meetsRequirement)
                            .executes(context -> {
                                context.getSource().sendMessage(subCommand.getUsageString());
                                return VarLightCommand.SUCCESS;
                            })
            );
        }

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
            String help = subCommand.getUsageString();

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
        ChatPaginator.ChatPage chatPage = ChatPaginator.paginate(getFullHelpRaw(sender), page);

        sender.sendMessage(ChatColor.GRAY + "-----------------------------------");
        sender.sendMessage(String.format("%sVarLight command help: %s[Page %d / %d]", ChatColor.GOLD, ChatColor.RESET, chatPage.getPageNumber(), chatPage.getTotalPages()));

        for (String line : chatPage.getLines()) {
            sender.sendMessage(line);
        }
    }
}
