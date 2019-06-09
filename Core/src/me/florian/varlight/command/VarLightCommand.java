package me.florian.varlight.command;

import me.florian.varlight.VarLightConfiguration;
import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.persistence.LightSourcePersistor;
import me.florian.varlight.persistence.PersistentLightSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.upperlevel.spigot.book.BookUtil;

import java.util.Optional;

public class VarLightCommand implements CommandExecutor {

    private VarLightPlugin plugin;
    private final String[] commandReference;
    private final ItemStack book;

    public VarLightCommand(VarLightPlugin plugin) {
        this.plugin = plugin;

        commandReference = new String[] {
                "VarLight command help:",
                "/varlight save: Save All Light sources in current world",
                "/varlight save <world>: Save All Light sources in the specified world",
                "/varlight save all: Save All Light sources",
                "/varlight autosave <interval>: Set the autosave interval",
                "",
                "/varlight getperm: Get the required permission node",
                "/varlight setperm <permission>: Set the required permission node",
                "/varlight unsetperm: Unset the required permission node",
                "",
                "/varlight whitelist add <world>: Add the specified world to the whitelist. " + ChatColor.BOLD + "Effective after restart!",
                "/varlight whitelist remove <world>: Remove the specified world to the whitelist. " + ChatColor.BOLD + "Effective after restart!",
                "/varlight whitelist list: List all whitelisted worlds",
                "",
                "/varlight blacklist add <world>: Add the specified world to the blacklist. " + ChatColor.BOLD + "Effective after restart!",
                "/varlight blacklist remove <world>: Remove the specified world to the blacklist. " + ChatColor.BOLD + "Effective after restart!",
                "/varlight blacklist list: List all blacklisted worlds"
        };

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
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight getperm")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight getperm\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Gets the current permission required to use varlight features\n\n").color(ChatColor.WHITE).build(),
                                BookUtil.TextBuilder.of("Required permission: varlight.admin.perm").color(ChatColor.WHITE).style(ChatColor.ITALIC).build()))
                                .build()
                ).newLine().newLine()
                        .add(
                                BookUtil.TextBuilder.of("Set permission")
                                        .style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText(BookUtil.TextBuilder.of("Click to run").color(ChatColor.RED).style(ChatColor.UNDERLINE).build()))
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight suggest /varlight setperm <permission>")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight setperm <permission>\n").color(ChatColor.WHITE).build(),
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
                                        .onClick(BookUtil.ClickAction.runCommand("/varlight unsetperm")).build()
                        ).add(
                        BookUtil.TextBuilder.of(" ?").color(ChatColor.BLUE).onHover(BookUtil.HoverAction.showText(
                                BookUtil.TextBuilder.of("/varlight unsetperm\n").color(ChatColor.WHITE).build(),
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

    private void showBook(Player player) {
        BookUtil.openPlayer(player, book);
    }

    private void printHelp(CommandSender commandSender) {
        commandSender.sendMessage(commandReference);
    }

    private boolean hasAnyVarLightPermission(CommandSender commandSender) {
        return commandSender.hasPermission("varlight.admin") ||
                commandSender.hasPermission("varlight.admin.save") ||
                commandSender.hasPermission("varlight.admin.perm") ||
                commandSender.hasPermission("varlight.admin.world");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (! "varlight".equalsIgnoreCase(command.getName())) {
            return true; // How would this happen?
        }

        if (! execute(commandSender, new ArgumentIterator(strings))) {
            printHelp(commandSender);
        }

        return true;
    }

    private boolean execute(CommandSender commandSender, ArgumentIterator args) {
        if (args.length == 0) {
            return false;
        }

        if (! hasAnyVarLightPermission(commandSender)) {
            return true;
        }

        switch (args.next().toLowerCase()) {
            case "suggest": {

                if (! (commandSender instanceof Player)) {
                    return true;
                }

                suggestCommand((Player) commandSender, args);
                return true;
            }

            case "migrate":
                return migrate(commandSender, args);
            case "save":
                return save(commandSender, args);
            case "autosave":
                return autosave(commandSender, args);
            case "getperm":
                return getPerm(commandSender, args);
            case "setperm":
                return setPerm(commandSender, args);
            case "unsetperm":
                return unsetPerm(commandSender, args);
            case "whitelist":
                return worldCommand(commandSender, VarLightConfiguration.WorldListType.WHITELIST, args);
            case "blacklist":
                return worldCommand(commandSender, VarLightConfiguration.WorldListType.BLACKLIST, args);
            case "book": {
                if (! (commandSender instanceof Player)) {
                    return true;
                }

                showBook((Player) commandSender);
                return true;
            }
            case "help":
            default:
                return false;
        }
    }

    private void suggestCommand(Player player, ArgumentIterator args) {
        plugin.getNmsAdapter().suggestCommand(player, args.join());
    }

    private boolean migrate(final CommandSender commandSender, final ArgumentIterator args) {
        final String node = "varlight.admin";

        if (! checkPerm(commandSender, node)) {
            return true;
        }

        class IntContainer {
            int i = 0;
        }

        IntContainer totalMigrated = new IntContainer(), totalSkipped = new IntContainer();

        broadcastResult(commandSender, "Starting migration...", node);

        LightSourcePersistor.getAllPersistors(plugin).forEach((p) -> {

            IntContainer migrated = new IntContainer(), skipped = new IntContainer();

            broadcastResult(commandSender, String.format("Migrating \"%s\"", p.getWorld().getName()), node);

            p.getAllLightSources().filter(PersistentLightSource::needsMigration).forEach(lightSource -> {
                if (! lightSource.getPosition().isChunkLoaded(lightSource.getWorld())) {
                    if (lightSource.getPosition().loadChunk(lightSource.getWorld(), false)) {
                        lightSource.update();
                        migrated.i++;
                    } else {
                        skipped.i++;
                    }
                } else {
                    lightSource.update();
                    migrated.i++;
                }
            });

            broadcastResult(commandSender, String.format("Migrated Light sources in world \"%s\" (migrated: %d, skipped: %d)", p.getWorld().getName(), migrated.i, skipped.i), node);

            totalMigrated.i += migrated.i;
            totalSkipped.i += skipped.i;
        });

        broadcastResult(commandSender, String.format("All Light sources migrated (total migrated: %d, skipped: %d)", totalMigrated.i, totalSkipped.i), node);

        return true;
    }

    private boolean save(CommandSender commandSender, ArgumentIterator args) {
        if (! checkPerm(commandSender, "varlight.admin.save")) {
            return true;
        }

        if (! args.hasNext()) {
            if (! (commandSender instanceof Player)) {
                sendPrefixedMessage(commandSender, "Only Players may use this command");
                return true;
            }

            Player player = (Player) commandSender;

            Optional<LightSourcePersistor> optLightSourcePersistor = LightSourcePersistor.getPersistor(plugin, player.getWorld());

            if (optLightSourcePersistor.isPresent()) {
                optLightSourcePersistor.get().save(player);
            } else {
                sendPrefixedMessage(player, String.format("No custom Light sources present in world \"%s\"", player.getWorld().getName()));
            }

            return true;
        }

        if ("all".equalsIgnoreCase(args.peek())) {
            LightSourcePersistor.getAllPersistors(plugin).forEach(persistor -> persistor.save(commandSender));
            return true;
        }

        World world = args.parseNext(Bukkit::getWorld);

        if (world == null) {
            sendPrefixedMessage(commandSender, "Could not find a world with that name");
        } else {
            Optional<LightSourcePersistor> optLightSourcePersistor = LightSourcePersistor.getPersistor(plugin, world);

            if (! optLightSourcePersistor.isPresent()) {
                sendPrefixedMessage(commandSender, String.format("No custom Light sources present in world \"%s\"", world.getName()));
            } else {
                optLightSourcePersistor.get().save(commandSender);
            }
        }

        return true;
    }

    private boolean autosave(CommandSender commandSender, ArgumentIterator args) {
        if (! checkPerm(commandSender, "varlight.admin.save")) {
            return true;
        }

        int newInterval;

        try {
            newInterval = args.parseNext(Integer::parseInt);
        } catch (NumberFormatException e) {
            sendPrefixedMessage(commandSender, e.getClass().getSimpleName() + ": " + e.getMessage());
            return true;
        }

        if (newInterval < 0) {
            sendPrefixedMessage(commandSender, "interval must be >= 0");
            return true;
        }

        plugin.getConfiguration().setAutosaveInterval(newInterval);
        plugin.initAutosave();

        if (newInterval > 0) {
            broadcastResult(commandSender, String.format("Updated Autosave interval to %d Minutes", newInterval), "varlight.admin.save");
        } else {
            broadcastResult(commandSender, "Disabled Autosave", "varlight.admin.save");
        }

        return true;
    }

    private boolean getPerm(CommandSender commandSender, ArgumentIterator args) {
        if (! checkPerm(commandSender, "varlight.admin.perm")) {
            return true;
        }

        sendPrefixedMessage(commandSender, String.format("Current required permission node: \"%s\"", plugin.getConfiguration().getRequiredPermissionNode()));
        return true;
    }

    private boolean setPerm(CommandSender commandSender, ArgumentIterator args) {
        if (! checkPerm(commandSender, "varlight.admin.perm")) {
            return true;
        }

        if (! args.hasNext()) {
            return false;
        }

        String permission = args.next();
        plugin.getConfiguration().setRequiredPermissionNode(permission);
        broadcastResult(commandSender, String.format("Required Permission Node updated to \"%s\"", permission), "varlight.admin.perm");
        return true;
    }

    private boolean unsetPerm(CommandSender commandSender, ArgumentIterator args) {
        if (! checkPerm(commandSender, "varlight.admin.perm")) {
            return true;
        }

        plugin.getConfiguration().setRequiredPermissionNode(null);
        broadcastResult(commandSender, "Unset Required Permission Node", "varlight.admin.perm");
        return true;
    }

    private boolean worldCommand(CommandSender commandSender, VarLightConfiguration.WorldListType worldListType, ArgumentIterator args) {
        if (! checkPerm(commandSender, "varlight.admin.world")) {
            return true;
        }

        if (! args.hasNext()) {
            return false;
        }

        String subCommand = args.next();

        if ("add".equalsIgnoreCase(subCommand)) {
            if (! args.hasNext()) {
                return false;
            }

            World world = args.parseNext(Bukkit::getWorld);

            if (world == null) {
                sendPrefixedMessage(commandSender, String.format("Could not find world \"%s\"", args.previous()));
                return true;
            }

            if (plugin.getConfiguration().addWorldToList(world, worldListType)) {
                broadcastResult(commandSender, String.format("Added world \"%s\" to the VarLight %s", world.getName(), worldListType.getName()), "varlight.admin.world");
            } else {
                sendPrefixedMessage(commandSender, String.format("World \"%s\" is already on the VarLight %s", world.getName(), worldListType.getName()));
            }

            return true;
        }

        if ("remove".equalsIgnoreCase(subCommand)) {
            if (! args.hasNext()) {
                return false;
            }

            World world = args.parseNext(Bukkit::getWorld);

            if (world == null) {
                sendPrefixedMessage(commandSender, String.format("Could not find world \"%s\"", args.previous()));
                return true;
            }

            if (plugin.getConfiguration().removeWorldFromList(world, worldListType)) {
                broadcastResult(commandSender, String.format("Removed world \"%s\" from the VarLight %s", world.getName(), worldListType.getName()), "varlight.admin.world");
            } else {
                sendPrefixedMessage(commandSender, String.format("World \"%s\" is not on the VarLight %s", world.getName(), worldListType.getName()));
            }

            return true;
        }

        if ("list".equalsIgnoreCase(subCommand)) {
            sendPrefixedMessage(commandSender, String.format("Worlds on the VarLight %s:", worldListType.getName()));

            for (World world : plugin.getConfiguration().getWorlds(worldListType)) {
                commandSender.sendMessage(String.format("   - \"%s\"", world.getName()));
            }

            return true;
        }

        return false;
    }

    private boolean checkPerm(CommandSender commandSender, String node) {
        if (! commandSender.hasPermission(node)) {
            commandSender.sendMessage(ChatColor.RED + "You do not have permission to use this command");
            return false;
        }

        return true;
    }

    private static void sendPrefixedMessage(CommandSender to, String message) {
        to.sendMessage(getPrefixedMessage(message));
    }

    private static String getPrefixedMessage(String message) {
        return String.format("[VarLight] %s", message);
    }

    private static void broadcastResult(CommandSender source, String message, String node) {
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

}
