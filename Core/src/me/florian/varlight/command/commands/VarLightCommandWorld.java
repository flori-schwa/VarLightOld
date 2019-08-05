package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightConfiguration;
import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class VarLightCommandWorld implements VarLightSubCommand {

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

        VarLightSubCommand.assertPermission(sender, "varlight.admin.world");

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
                    VarLightSubCommand.sendPrefixedMessage(sender, String.format("Could not find world \"%s\"", args.previous()));
                    return true;
                }

                if (plugin.getConfiguration().addWorldToList(world, worldListType)) {
                    VarLightSubCommand.broadcastResult(sender, String.format("Added world \"%s\" to the VarLight %s", world.getName(), worldListType.getName()), "varlight.admin.world");
                } else {
                    VarLightSubCommand.sendPrefixedMessage(sender, String.format("World \"%s\" is already on the VarLight %s", world.getName(), worldListType.getName()));
                }

                return true;
            }

            case "remove": {
                if (!args.hasNext()) {
                    return false;
                }

                World world = args.parseNext(Bukkit::getWorld);

                if (world == null) {
                    VarLightSubCommand.sendPrefixedMessage(sender, String.format("Could not find world \"%s\"", args.previous()));
                    return true;
                }

                if (plugin.getConfiguration().removeWorldFromList(world, worldListType)) {
                    VarLightSubCommand.broadcastResult(sender, String.format("Removed world \"%s\" from the VarLight %s", world.getName(), worldListType.getName()), "varlight.admin.world");
                } else {
                    VarLightSubCommand.sendPrefixedMessage(sender, String.format("World \"%s\" is not on the VarLight %s", world.getName(), worldListType.getName()));
                }

                return true;
            }

            case "list": {
                VarLightSubCommand.sendPrefixedMessage(sender, String.format("Worlds on the VarLight %s:", worldListType.getName()));

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
}
