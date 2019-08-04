package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightSubCommand;
import me.florian.varlight.command.exception.VarLightCommandException;
import me.florian.varlight.event.LightUpdateEvent;
import me.florian.varlight.persistence.LightSourcePersistor;
import me.florian.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class VarLightCommandCreate implements VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandCreate(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }


    @Override
    public void sendHelp(CommandSender sender) {
        sender.sendMessage("/varlight create <world> <x> <y> <z> <lightlevel>: Update the light level at the given position");
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightSubCommand.assertPermission(sender, "varlight.admin");

        if (!args.hasParameters(5)) {
            return false;
        }

        final World world = args.parseNext(Bukkit::getWorld);

        if (world == null) {
            VarLightSubCommand.sendPrefixedMessage(sender, String.format("Could not find a world with the name \"%s\"", args.previous()));
            return true;
        }

        if (!LightSourcePersistor.hasPersistor(plugin, world)) {
            VarLightSubCommand.sendPrefixedMessage(sender, "VarLight is not active in that world!");
            return true;
        }

        final LightSourcePersistor lightSourcePersistor = LightSourcePersistor.getPersistor(plugin, world).get();

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
            VarLightSubCommand.sendPrefixedMessage(sender, String.format("Light level out of range, allowed: 0 <= n <= 15, got: %d", lightLevel));
            return false;
        }

        final Location toUpdate = new Location(world, x, y, z);
        final int fromLight = LightSourcePersistor.getEmittingLightLevel(plugin, toUpdate);

        if (!world.isChunkLoaded(toUpdate.getBlockX() >> 4, toUpdate.getBlockZ() >> 4)) {
            VarLightSubCommand.sendPrefixedMessage(sender, "That part of the world is not loaded");
            return true;
        }

        if (!plugin.getNmsAdapter().isValidBlock(world.getBlockAt(toUpdate))) {
            VarLightSubCommand.sendPrefixedMessage(sender, String.format("%s cannot be used as a custom light source!", world.getBlockAt(toUpdate).getType().name()));
            return true;
        }

        LightUpdateEvent lightUpdateEvent = new LightUpdateEvent(world.getBlockAt(toUpdate), fromLight, lightLevel);
        Bukkit.getPluginManager().callEvent(lightUpdateEvent);

        if (lightUpdateEvent.isCancelled()) {
            VarLightSubCommand.sendPrefixedMessage(sender, "The Light update event was cancelled!");
            return true;
        }

        lightSourcePersistor.getOrCreatePersistentLightSource(new IntPosition(toUpdate))
                .setEmittingLight(lightUpdateEvent.getToLight());

        plugin.getNmsAdapter().updateBlockLight(toUpdate, lightUpdateEvent.getToLight());
        VarLightSubCommand.broadcastResult(sender, String.format("Updated Light level at [%d, %d, %d] in world \"%s\" from %d to %d",
                toUpdate.getBlockX(), toUpdate.getBlockY(), toUpdate.getBlockZ(), world.getName(), lightUpdateEvent.getFromLight(), lightUpdateEvent.getToLight()), "varlight.admin");

        return true;
    }
}
