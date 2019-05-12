package me.florian.varlight.command;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.persistence.LightSourcePersistor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class VarLightCommand implements CommandExecutor {

    private VarLightPlugin plugin;

    public VarLightCommand(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    private void printHelp(CommandSender commandSender) {
        commandSender.sendMessage(new String[] {
                "VarLight command help:",
                "/varlight save: Save All Light sources in current world",
                "/varlight save <world>: Save All Light sources in the specified world",
                "/varlight save all: Save All Light sources",
                "/varlight autosave <interval>: Set the autosave interval. Effective after restart",
                "",
                "/varlight getperm: Get the required permission node",
                "/varlight setperm <permission>: Set the required permission node",
                "/varlight unsetperm: Unset the required permission node"
        });
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (! "varlight".equalsIgnoreCase(command.getName())) {
            return true; // How would this happen?
        }

        if (! execute(commandSender, new ArgumentStream(strings))) {
            printHelp(commandSender);
        }

        return true;
    }

    private boolean execute(CommandSender commandSender, ArgumentStream args) {
        if (args.length == 0) {
            return false;
        }

        switch (args.get().toLowerCase()) {
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
            case "help":
            default:
                return false;
        }
    }

    private boolean save(CommandSender commandSender, ArgumentStream args) {
        if (! checkPerm(commandSender, "varlight.admin.save")) {
            return true;
        }

        if (! args.hasNext()) {
            if (! (commandSender instanceof Player)) {
                commandSender.sendMessage("Only Players may use this command");
                return true;
            }

            Player player = (Player) commandSender;

            Optional<LightSourcePersistor> optLightSourcePersistor = LightSourcePersistor.getPersistor(plugin, player.getWorld());

            if (optLightSourcePersistor.isPresent()) {
                optLightSourcePersistor.get().save(player);
            } else {
                player.sendMessage(String.format("No custom Light sources present in world \"%s\"", player.getWorld().getName()));
            }

            return true;
        }

        if ("all".equalsIgnoreCase(args.peek())) {
            LightSourcePersistor.getAllPersistors(plugin).forEach(persistor -> persistor.save(commandSender));
            return true;
        }

        World world = args.parseNext(Bukkit::getWorld);

        if (world == null) {
            commandSender.sendMessage("Could not find a world with that name");
        } else {
            Optional<LightSourcePersistor> optLightSourcePersistor = LightSourcePersistor.getPersistor(plugin, world);

            if (! optLightSourcePersistor.isPresent()) {
                commandSender.sendMessage(String.format("No custom Light sources present in world \"%s\"", world.getName()));
            } else {
                optLightSourcePersistor.get().save(commandSender);
            }
        }

        return true;
    }

    private boolean autosave(CommandSender commandSender, ArgumentStream args) {
        if (! checkPerm(commandSender, "varlight.admin.save")) {
            return true;
        }

        int newInterval;

        try {
            newInterval = args.parseNext(Integer::parseInt);
        } catch (NumberFormatException e) {
            commandSender.sendMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            return true;
        }

        if (newInterval <= 0) {
            commandSender.sendMessage("interval must be > 0");
            return true;
        }

        plugin.getConfiguration().setAutosaveInterval(newInterval);
        broadcastResult(commandSender, String.format("Updated autosave interval to %d Minuted", newInterval), "varlight.admin.save");
        return true;
    }

    private boolean getPerm(CommandSender commandSender, ArgumentStream args) {
        if (! checkPerm(commandSender, "varlight.admin.perm")) {
            return true;
        }

        commandSender.sendMessage(String.format("Current required permission node: \"%s\"", plugin.getConfiguration().getRequiredPermissionNode()));
        return true;
    }

    private boolean setPerm(CommandSender commandSender, ArgumentStream args) {
        if (! checkPerm(commandSender, "varlight.admin.perm")) {
            return true;
        }

        if (! args.hasNext()) {
            return false;
        }

        String permission = args.get();
        plugin.getConfiguration().setRequiredPermissionNode(permission);
        broadcastResult(commandSender, String.format("Required Permission Node updated to \"%s\"", permission), "varlight.admin.perm");
        return true;
    }

    private boolean unsetPerm(CommandSender commandSender, ArgumentStream args) {
        if (! checkPerm(commandSender, "varlight.admin.perm")) {
            return true;
        }

        plugin.getConfiguration().setRequiredPermissionNode(null);
        broadcastResult(commandSender, "Unset Required Permission Node", "varlight.admin.perm");
        return true;
    }

    private boolean checkPerm(CommandSender commandSender, String node) {
        if (! commandSender.hasPermission(node)) {
            commandSender.sendMessage(ChatColor.RED + "You do not have permission to use this command");
            return false;
        }

        return true;
    }

    private static final void broadcastResult(CommandSender source, String message, String node) {
        String msg = String.format("%s: %s", source.getName(), message);
        String formatted = ChatColor.GRAY + "" + ChatColor.ITALIC + String.format("[%s]", msg);
        source.sendMessage(message);

        Bukkit.getPluginManager().getPermissionSubscriptions(node).stream().filter(p -> p != source && p instanceof CommandSender).forEach(p -> {
            if (p instanceof ConsoleCommandSender) {
                ((ConsoleCommandSender) p).sendMessage(msg);
            } else {
                ((CommandSender) p).sendMessage(formatted);
            }
        });

    }

}
