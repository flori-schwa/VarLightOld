package me.florian.varlight.command;

import me.florian.varlight.VarLightConfiguration;
import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.commands.*;
import me.florian.varlight.command.exception.VarLightCommandException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.upperlevel.spigot.book.BookUtil;

import java.util.*;

public class VarLightCommand implements CommandExecutor, TabCompleter {

    private VarLightPlugin plugin;

    private final Map<String, VarLightSubCommand> subCommands = new HashMap<>();
    private final ItemStack book;

    private final VarLightCommandHelp helpCommand;

    public VarLightCommand(VarLightPlugin plugin) {
        this.plugin = plugin;

        registerCommand(new VarLightCommandSuggest(plugin));
        registerCommand(new VarLightCommandDebug(plugin));

        registerCommand(new VarLightCommandSave(plugin));
        registerCommand(new VarLightCommandAutosave(plugin));

        registerCommand(new VarLightCommandPermission(plugin));

        registerCommand(new VarLightCommandUpdate(plugin));
        registerCommand(new VarLightCommandReload(plugin));
        registerCommand(new VarLightCommandMigrate(plugin));

        registerCommand(new VarLightCommandWorld("whitelist", VarLightConfiguration.WorldListType.WHITELIST, plugin));
        registerCommand(new VarLightCommandWorld("blacklist", VarLightConfiguration.WorldListType.BLACKLIST, plugin));

        registerCommand(new VarLightSubCommand() {
            @Override
            public String getName() {
                return "book";
            }

            @Override
            public String getSyntax() {
                return "";
            }

            @Override
            public String getDescription() {
                return "Opens the interactive command book";
            }

            @Override
            public boolean execute(CommandSender sender, ArgumentIterator args) {
                if (sender instanceof Player) {
                    BookUtil.openPlayer(((Player) sender), book);
                }

                return true;
            }
        });

        registerCommand(helpCommand = new VarLightCommandHelp(this));

        // region Book definition

        final String arrowRight = "\u00bb";
        final String arrowLeft = "\u00ab";

        book = BookUtil.writtenBook().author("shawlaf").pages(
                new BookUtil.PageBuilder()
                        .add(
                                BookUtil.TextBuilder.of("VarLight Command Help")
                                        .color(ChatColor.GOLD).build()
                        )
                        .newLine()
                        .newLine()
                        .newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowRight + " Save").style(ChatColor.UNDERLINE)
                                        .onClick(BookUtil.ClickAction.changePage(2)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump to save commands").color(ChatColor.WHITE).build()
                                )).build()
                        ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowRight + " Permission").style(ChatColor.UNDERLINE)
                                        .onClick(BookUtil.ClickAction.changePage(3)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump to permission commands").color(ChatColor.WHITE).build()
                                )).build()
                        ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowRight + " Whitelist").style(ChatColor.UNDERLINE)
                                        .onClick(BookUtil.ClickAction.changePage(4)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump to whitelist commands").color(ChatColor.WHITE).build()
                                )).build()
                        ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowRight + " Blacklist").style(ChatColor.UNDERLINE)
                                        .onClick(BookUtil.ClickAction.changePage(5)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump to blacklist commands").color(ChatColor.WHITE).build()
                                )).build()
                        )
                        .build(),
                new BookUtil.PageBuilder()
                        .add(
                                BookUtil.TextBuilder.of("Save Commands").color(ChatColor.GOLD).build()
                        )
                        .newLine()
                        .newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowLeft + " Back to index").style(ChatColor.UNDERLINE).color(ChatColor.DARK_BLUE).onClick(
                                        BookUtil.ClickAction.changePage(1)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump back to index").color(ChatColor.WHITE).build()
                                )).build()
                        ).newLine().newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("All in current world")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight save")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight save\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Saves all Light sources in current world\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Can only be ran as a Player!\n\n").color(ChatColor.WHITE).style(ChatColor.UNDERLINE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.save").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("All in specific world")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight save <world>")).build()
                        ).add(BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                        BookUtil.TextBuilder.of("/varlight save <world>\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Saves all Light sources in the specified world\n\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Arguments\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("    <world>: World, whose Light sources should be saved\n\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Required permission: varlight.admin.save").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                        .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("All in all worlds")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight save all")).build()
                        ).add(BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                        BookUtil.TextBuilder.of("/varlight save all\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Saves all Light sources in all worlds\n\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Required permission: varlight.admin.save").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                        .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Set Autosave")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight autosave <interval>")).build()
                        ).add(BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                        BookUtil.TextBuilder.of("/varlight autosave <interval>\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Update the autosave interval\n\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Arguments\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("    <interval>: New Autosave interval in minutes, set to 0 to disable autosave\n\n").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of("Required permission: varlight.admin.save").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                        .build()
                ).newLine().newLine()

                        .build(),
                new BookUtil.PageBuilder()
                        .add(
                                BookUtil.TextBuilder.of("Permission Commands").color(ChatColor.GOLD).build()
                        )
                        .newLine()
                        .newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowLeft + " Back to index").style(ChatColor.UNDERLINE).color(ChatColor.DARK_BLUE).onClick(
                                        BookUtil.ClickAction.changePage(1)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump back to index").color(ChatColor.WHITE).build()
                                )).build()
                        ).newLine().newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Get permission")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight perm get")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight perm get\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Gets the current permission required to use varlight features\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.perm").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Set permission")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight perm set <permission>")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight perm set <permission>\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Sets the required permission to use varlight features\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Arguments\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("    <permission>: The new permission required to use varlight features\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.perm").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Unset permission")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight perm unset")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight perm unset\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Unsets the required permission to use VarLight features allowing everyone to use VarLight features\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.perm").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .build(),
                new BookUtil.PageBuilder()
                        .add(
                                BookUtil.TextBuilder.of("Whitelist Commands").color(ChatColor.GOLD).build()
                        )
                        .add(
                                BookUtil.TextBuilder.of(" ?")
                                        .color(ChatColor.BLUE)
                                        .onHover(BookUtil.HoverAction.showText(
                                                BookUtil.TextBuilder.of("How does the Whitelist/Blacklist work?\n\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("Does a Whitelist exist?\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("   Yes -> Use that list\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("   No  -> Get all worlds and use that list\n\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("Does a Blacklist exists?\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("   Yes -> Remove every entry in the Blacklist from the List\n\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of(" => Enable VarLight with all worlds in the List").color(ChatColor.WHITE).build()
                                        )).build()
                        )
                        .newLine()
                        .newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowLeft + " Back to index").style(ChatColor.UNDERLINE).color(ChatColor.DARK_BLUE).onClick(
                                        BookUtil.ClickAction.changePage(1)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump back to index").color(ChatColor.WHITE).build()
                                )).build()
                        ).newLine().newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Whitelist world")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight whitelist add <world>")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight whitelist add <world>\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Add a world to the VarLight whitelist\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("EFFECTIVE AFTER RESTART\n\n").style(ChatColor.BOLD).color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Arguments\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("    <world>: The world to be added to the whitelist\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.world").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Un-Whitelist world")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight whitelist remove <world>")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight whitelist remove <world>\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Remove a world from the VarLight whitelist\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("EFFECTIVE AFTER RESTART\n\n").style(ChatColor.BOLD).color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Arguments\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("    <world>: The world to be removed from the whitelist\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.world").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("List Whitelisted")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight whitelist list")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight whitelist list\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("List all worlds on the VarLight whitelist\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.world").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .build(),
                new BookUtil.PageBuilder()
                        .add(
                                BookUtil.TextBuilder.of("Blacklist Commands").color(ChatColor.GOLD).build()
                        )
                        .add(
                                BookUtil.TextBuilder.of(" ?")
                                        .color(ChatColor.BLUE)
                                        .onHover(BookUtil.HoverAction.showText(
                                                BookUtil.TextBuilder.of("How does the Whitelist/Blacklist work?\n\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("Does a Whitelist exist?\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("   Yes -> Use that list\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("   No  -> Get all worlds and use that list\n\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("Does a Blacklist exists?\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of("   Yes -> Remove every entry in the Blacklist from the List\n\n").color(ChatColor.WHITE).build(),
                                                BookUtil.TextBuilder.of(" => Enable VarLight with all worlds in the List").color(ChatColor.WHITE).build()
                                        )).build()
                        )
                        .newLine()
                        .newLine()
                        .add(
                                BookUtil.TextBuilder.of(arrowLeft + " Back to index").style(ChatColor.UNDERLINE).color(ChatColor.DARK_BLUE).onClick(
                                        BookUtil.ClickAction.changePage(1)).onHover(BookUtil.HoverAction.showText(
                                        BookUtil.TextBuilder.of("Jump back to index").color(ChatColor.WHITE).build()
                                )).build()
                        ).newLine().newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Blacklist world")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight blacklist add <world>")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight blacklist add <world>\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Add a world to the VarLight blacklist\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("EFFECTIVE AFTER RESTART\n\n").style(ChatColor.BOLD).color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Arguments\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("    <world>: The world to be added to the blacklist\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.world").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Un-Blacklist world")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight blacklist remove <world>")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight blacklist remove <world>\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Remove a world from the VarLight blacklist\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("EFFECTIVE AFTER RESTART\n").style(ChatColor.BOLD).color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Arguments\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("    <world>: The world to be removed from the blacklist\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.world").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("List Blacklisted")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight blacklist list")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight blacklist list\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("List all worlds on the VarLight blacklist\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.world").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .build()
        ).build();

        // endregion
    }

    public void registerCommand(VarLightSubCommand subCommand) {
        subCommands.put(subCommand.getName(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!"varlight".equalsIgnoreCase(command.getName())) {
            return true; // How would this happen?
        }

        if (args.length == 0) {
            helpCommand.showHelp(commandSender);
            return true;
        }

        final ArgumentIterator arguments = new ArgumentIterator(args);
        final VarLightSubCommand subCommand = arguments.parseNext(subCommands::get);

        if (subCommand == null) {
            helpCommand.showHelp(commandSender);
            return true;
        }

        try {
            if (!subCommand.execute(commandSender, arguments)) {
                commandSender.sendMessage(subCommand.getCommandHelp());
                return true;
            }
        } catch (VarLightCommandException e) {
            sendPrefixedMessage(commandSender, e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String commandLabel, String[] args) {
        if (!"varlight".equalsIgnoreCase(command.getName())) {
            return null; // wot
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();

            for (String label : getRegisteredCommands().keySet()) {
                if (label.startsWith(args[0])) {
                    list.add(label);
                }
            }

            return list;
        } else {
            final VarLightSubCommand subCommand = subCommands.get(args[0]);

            if (subCommand == null) {
                return null;
            }

            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);

            return subCommand.tabComplete(commandSender, new ArgumentIterator(newArgs));
        }
    }

    public Map<String, VarLightSubCommand> getRegisteredCommands() {
        return Collections.unmodifiableMap(subCommands);
    }

    public static void assertPermission(CommandSender commandSender, String node) {
        if (!commandSender.hasPermission(node)) {
            throw new VarLightCommandException(ChatColor.RED + "You do not have permission to use this command");
        }
    }

    public static void sendPrefixedMessage(CommandSender to, String message) {
        to.sendMessage(getPrefixedMessage(message));
    }

    public static String getPrefixedMessage(String message) {
        return String.format("[VarLight] %s", message);
    }

    public static void broadcastResult(CommandSender source, String message, String node) {
        String msg = String.format("%s: %s", source.getName(), getPrefixedMessage(message));
        String formatted = ChatColor.GRAY + "" + ChatColor.ITALIC + String.format("[%s]", msg);
        source.sendMessage(getPrefixedMessage(message));

        Bukkit.getPluginManager().getPermissionSubscriptions(node).stream().filter(p -> p != source && p instanceof CommandSender).forEach(p -> {
            if (p instanceof ConsoleCommandSender) {
                ((ConsoleCommandSender) p).sendMessage(msg);
            } else {
                ((CommandSender) p).sendMessage(formatted);
            }
        });
    }

    public static List<String> suggestBlockPosition(Player player, String currentArg, int completedCoords) {

        final List<String> suggestions = new ArrayList<>();

        final int[] coords = getCoordinatesLookingAt(player);

        if (coords.length == 0) {
            return suggestions;
        }

        final int[] toSuggest = new int[3 - completedCoords];

        System.arraycopy(coords, completedCoords, toSuggest, 0, toSuggest.length);

        for (int i = 0; i < toSuggest.length; i++) {
            StringBuilder builder = new StringBuilder();

            for (int j = 0; j <= i; j++) {
                builder.append(toSuggest[j]);
                builder.append(" ");
            }

            String suggestion = builder.toString().trim();

            if (suggestion.startsWith(currentArg)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    private static int[] getCoordinatesLookingAt(Player player) {
        Block targetBlock = player.getTargetBlockExact(10, FluidCollisionMode.NEVER);

        if (targetBlock == null) {
            return new int[0];
        }

        return new int[]{
                targetBlock.getX(),
                targetBlock.getY(),
                targetBlock.getZ()
        };
    }

    public static List<String> suggestChoice(String currentArg, String... choices) {
        final List<String> suggestions = new ArrayList<>();

        for (String choice : choices) {
            if (choice.startsWith(currentArg)) {
                suggestions.add(choice);
            }
        }

        return suggestions;
    }
}
