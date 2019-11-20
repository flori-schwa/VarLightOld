package me.shawlaf.varlight.command.commands;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.ArgumentIterator;
import me.shawlaf.varlight.command.CommandSuggestions;
import me.shawlaf.varlight.command.VarLightCommand;
import me.shawlaf.varlight.command.VarLightSubCommand;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class VarLightCommandSave extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandSave(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getSyntax() {
        return " [all/<world>]";
    }

    @Override
    public String getDescription() {
        return "Save all custom light sources in the current world, the specified world or all worlds";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightCommand.assertPermission(sender, "varlight.admin.save");

        if (!args.hasNext()) {
            if (!(sender instanceof Player)) {
                VarLightCommand.sendPrefixedMessage(sender, "Only Players may use this command");
                return true;
            }

            Player player = (Player) sender;

            WorldLightSourceManager manager = WorldLightSourceManager.getManager(plugin, player.getWorld());

            if (manager != null) {
                manager.save(player);
            } else {
                VarLightCommand.sendPrefixedMessage(player, String.format("No custom Light sources present in world \"%s\"", player.getWorld().getName()));
            }

            return true;
        }

        if ("all".equalsIgnoreCase(args.peek())) {
            WorldLightSourceManager.getAllManager(plugin).forEach(persistor -> persistor.save(sender));
            return true;
        }

        World world = args.parseNext(Bukkit::getWorld);

        if (world == null) {
            VarLightCommand.sendPrefixedMessage(sender, "Could not find a world with that name");
        } else {
            WorldLightSourceManager manager = WorldLightSourceManager.getManager(plugin, world);

            if (manager == null) {
                VarLightCommand.sendPrefixedMessage(sender, String.format("VarLight is not active in world \"%s\"", world.getName()));
            } else {
                manager.save(sender);
            }
        }

        return true;
    }

    @Override
    public void tabComplete(CommandSuggestions commandSuggestions) {
        if (commandSuggestions.getArgumentCount() != 1) {
            return;
        }

        commandSuggestions
                .addSuggestion("all")
                .suggestChoices(Bukkit.getWorlds().stream()
                        .filter(w -> WorldLightSourceManager.hasManager(plugin, w))
                        .map(World::getName)
                        .collect(Collectors.toSet())
                );
    }
}
