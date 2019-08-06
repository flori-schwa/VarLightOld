package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightConfiguration;
import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightCommand;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

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
    public List<String> tabComplete(CommandSender sender, ArgumentIterator args) {
        final int arguments = args.length;

        switch (arguments) {
            case 1: {
                return VarLightCommand.suggestChoice(args.get(0), "add", "remove", "list");
            }
            case 2: {
                switch (args.get(0).toLowerCase()) {
                    case "add": {
                        return VarLightCommand.suggestChoice(args.get(1),
                                Bukkit.getWorlds().stream()
                                        .filter(w -> !plugin.getConfiguration().getWorlds(worldListType).contains(w))
                                        .map(World::getName).toArray(String[]::new));
                    }

                    case "remove": {
                        return VarLightCommand.suggestChoice(args.get(1),
                                plugin.getConfiguration().getWorlds(worldListType).stream().map(World::getName).toArray(String[]::new));
                    }

                    default: {
                        return new ArrayList<>();
                    }
                }
            }
        }

        return new ArrayList<>();
    }
}