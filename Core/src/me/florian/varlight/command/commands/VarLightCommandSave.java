package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightSubCommand;
import me.florian.varlight.persistence.LightSourcePersistor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class VarLightCommandSave implements VarLightSubCommand {

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
        VarLightSubCommand.assertPermission(sender, "varlight.admin.save");

        if (!args.hasNext()) {
            if (!(sender instanceof Player)) {
                VarLightSubCommand.sendPrefixedMessage(sender, "Only Players may use this command");
                return true;
            }

            Player player = (Player) sender;

            Optional<LightSourcePersistor> optLightSourcePersistor = LightSourcePersistor.getPersistor(plugin, player.getWorld());

            if (optLightSourcePersistor.isPresent()) {
                optLightSourcePersistor.get().save(player);
            } else {
                VarLightSubCommand.sendPrefixedMessage(player, String.format("No custom Light sources present in world \"%s\"", player.getWorld().getName()));
            }

            return true;
        }

        if ("all".equalsIgnoreCase(args.peek())) {
            LightSourcePersistor.getAllPersistors(plugin).forEach(persistor -> persistor.save(sender));
            return true;
        }

        World world = args.parseNext(Bukkit::getWorld);

        if (world == null) {
            VarLightSubCommand.sendPrefixedMessage(sender, "Could not find a world with that name");
        } else {
            Optional<LightSourcePersistor> optLightSourcePersistor = LightSourcePersistor.getPersistor(plugin, world);

            if (!optLightSourcePersistor.isPresent()) {
                VarLightSubCommand.sendPrefixedMessage(sender, String.format("VarLight is not active in world \"%s\"", world.getName()));
            } else {
                optLightSourcePersistor.get().save(sender);
            }
        }

        return true;
    }
}
