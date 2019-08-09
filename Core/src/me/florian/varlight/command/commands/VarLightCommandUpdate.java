package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.CommandSuggestions;
import me.florian.varlight.command.VarLightCommand;
import me.florian.varlight.command.VarLightSubCommand;
import me.florian.varlight.command.exception.VarLightCommandException;
import me.florian.varlight.event.LightUpdateEvent;
import me.florian.varlight.persistence.LightSourcePersistor;
import me.florian.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VarLightCommandUpdate extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandUpdate(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "update";
    }
    
    @Override
    public String getSyntax() {
        return " <x> <y> <z> <light level> [world (only if using console)]";
    }

    @Override
    public String getDescription() {
        return "Update the light level at the given position";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightCommand.assertPermission(sender, "varlight.admin");

        if (!args.hasParameters(4)) {
            return false;
        }

        final int x, y, z, lightLevel;

        try {
            x = args.parseNext(Integer::parseInt);
            y = args.parseNext(Integer::parseInt);
            z = args.parseNext(Integer::parseInt);

            lightLevel = args.parseNext(Integer::parseInt);
        } catch (NumberFormatException e) {
            throw new VarLightCommandException(String.format("Malformed input: %s", e.getMessage()), e);
        }

        if (lightLevel < 0 || lightLevel > 15) {
            VarLightCommand.sendPrefixedMessage(sender, String.format("Light level out of range, allowed: 0 <= n <= 15, got: %d", lightLevel));
            return false;
        }

        World world;

        if (sender instanceof Player && !args.hasNext()) {
            world = ((Player) sender).getWorld();
        } else {
            if (!args.hasNext()) {
                return false;
            }

            world = args.parseNext(Bukkit::getWorld);
        }

        if (world == null) {
            VarLightCommand.sendPrefixedMessage(sender, String.format("Could not find a world with the name \"%s\"", args.previous()));
            return true;
        }

        if (!LightSourcePersistor.hasPersistor(plugin, world)) {
            VarLightCommand.sendPrefixedMessage(sender, "VarLight is not active in that world!");
            return true;
        }

        final LightSourcePersistor lightSourcePersistor = LightSourcePersistor.getPersistor(plugin, world).get();

        final Location toUpdate = new Location(world, x, y, z);
        final int fromLight = LightSourcePersistor.getEmittingLightLevel(plugin, toUpdate);

        if (!world.isChunkLoaded(toUpdate.getBlockX() >> 4, toUpdate.getBlockZ() >> 4)) {
            VarLightCommand.sendPrefixedMessage(sender, "That part of the world is not loaded");
            return true;
        }

        if (!plugin.getNmsAdapter().isValidBlock(world.getBlockAt(toUpdate))) {
            VarLightCommand.sendPrefixedMessage(sender, String.format("%s cannot be used as a custom light source!", world.getBlockAt(toUpdate).getType().name()));
            return true;
        }

        LightUpdateEvent lightUpdateEvent = new LightUpdateEvent(world.getBlockAt(toUpdate), fromLight, lightLevel);
        Bukkit.getPluginManager().callEvent(lightUpdateEvent);

        if (lightUpdateEvent.isCancelled()) {
            VarLightCommand.sendPrefixedMessage(sender, "The Light update event was cancelled!");
            return true;
        }

        lightSourcePersistor.getOrCreatePersistentLightSource(new IntPosition(toUpdate))
                .setEmittingLight(lightUpdateEvent.getToLight());

        plugin.getNmsAdapter().updateBlockLight(toUpdate, lightUpdateEvent.getToLight());
        VarLightCommand.broadcastResult(sender, String.format("Updated Light level at [%d, %d, %d] in world \"%s\" from %d to %d",
                toUpdate.getBlockX(), toUpdate.getBlockY(), toUpdate.getBlockZ(), world.getName(), lightUpdateEvent.getFromLight(), lightUpdateEvent.getToLight()), "varlight.admin");

        return true;
    }

    @Override
    public void tabComplete(CommandSuggestions suggestions) {
        if (suggestions.getArgumentCount() <= 3) {
            suggestions.suggestBlockPosition(suggestions.getArgumentCount() - 1);
        } else if (suggestions.getArgumentCount() == 4) {
            suggestions.suggestChoices(IntStream.range(0, 16).mapToObj(String::valueOf).toArray(String[]::new));
        } else if (suggestions.getArgumentCount() == 5) {
            suggestions.suggestChoices(Bukkit.getWorlds().stream()
                    .filter(w -> LightSourcePersistor.hasPersistor(plugin, w))
                    .map(World::getName)
                    .collect(Collectors.toSet())
            );
        }
    }
}
