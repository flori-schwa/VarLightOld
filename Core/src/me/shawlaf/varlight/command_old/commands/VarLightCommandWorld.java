package me.shawlaf.varlight.command_old.commands;

import me.shawlaf.varlight.VarLightConfiguration;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command_old.ArgumentIterator;
import me.shawlaf.varlight.command_old.CommandSuggestions;
import me.shawlaf.varlight.command_old.VarLightCommand;
import me.shawlaf.varlight.command_old.VarLightSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.stream.Collectors;

@Deprecated
public class VarLightCommandWorld extends VarLightSubCommand {

    private final String label;
    private final VarLightConfiguration.WorldListType worldListType;
    private final VarLightPlugin plugin;

    public VarLightCommandWorld(String label, VarLightConfiguration.WorldListType worldListType, VarLightPlugin plugin) {
        this.label = label;
        this.worldListType = worldListType;
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return label;
    }

    @Override
    public String getSyntax() {
        return " add/remove/list [world]";
    }

    @Override
    public String getDescription() {
        return String.format("Add/Remove worlds from the %s or list all worlds on the %s", worldListType.getName(), worldListType.getName());
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {

        VarLightCommand.assertPermission(sender, "varlight.admin.world");

        if (!args.hasNext()) {
            return false;
        }

        switch (args.next().toLowerCase()) {
            case "add": {
                if (!args.hasNext()) {
                    return false;
                }

                World world = args.parseNext(Bukkit::getWorld);

                if (world == null) {
                    VarLightCommand.sendPrefixedMessage(sender, String.format("Could not find world \"%s\"", args.previous()));
                    return true;
                }

                if (plugin.getConfiguration().addWorldToList(world, worldListType)) {
                    VarLightCommand.broadcastResult(sender, String.format("Added world \"%s\" to the VarLight %s", world.getName(), worldListType.getName()), "varlight.admin.world");
                } else {
                    VarLightCommand.sendPrefixedMessage(sender, String.format("World \"%s\" is already on the VarLight %s", world.getName(), worldListType.getName()));
                }

                return true;
            }

            case "remove": {
                if (!args.hasNext()) {
                    return false;
                }

                World world = args.parseNext(Bukkit::getWorld);

                if (world == null) {
                    VarLightCommand.sendPrefixedMessage(sender, String.format("Could not find world \"%s\"", args.previous()));
                    return true;
                }

                if (plugin.getConfiguration().removeWorldFromList(world, worldListType)) {
                    VarLightCommand.broadcastResult(sender, String.format("Removed world \"%s\" from the VarLight %s", world.getName(), worldListType.getName()), "varlight.admin.world");
                } else {
                    VarLightCommand.sendPrefixedMessage(sender, String.format("World \"%s\" is not on the VarLight %s", world.getName(), worldListType.getName()));
                }

                return true;
            }

            case "list": {
                VarLightCommand.sendPrefixedMessage(sender, String.format("Worlds on the VarLight %s:", worldListType.getName()));

                for (World world : plugin.getConfiguration().getWorlds(worldListType)) {
                    sender.sendMessage(String.format("   - \"%s\"", world.getName()));
                }

                return true;
            }

            default: {
                return false;
            }
        }
    }

    @Override
    public void tabComplete(CommandSuggestions commandSuggestions) {
        switch (commandSuggestions.getArgumentCount()) {
            case 1: {
                commandSuggestions.suggestChoices("add", "remove", "list");
                return;
            }

            case 2: {
                switch (commandSuggestions.getArgs()[0].toLowerCase()) {
                    case "add": {
                        commandSuggestions.suggestChoices(
                                Bukkit.getWorlds().stream()
                                        .filter(w -> !plugin.getConfiguration().getWorlds(worldListType).contains(w))
                                        .map(World::getName).collect(Collectors.toSet())
                        );

                        return;
                    }

                    case "remove": {
                        commandSuggestions.suggestChoices(plugin.getConfiguration().getWorlds(worldListType).stream()
                                .map(World::getName)
                                .collect(Collectors.toSet())
                        );
                    }
                }
            }
        }
    }
}
