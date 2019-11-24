package me.shawlaf.varlight.command.commands;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.ArgumentIterator;
import me.shawlaf.varlight.command.CommandSuggestions;
import me.shawlaf.varlight.command.VarLightCommand;
import me.shawlaf.varlight.command.VarLightSubCommand;
import me.shawlaf.varlight.command.exception.VarLightCommandException;
import me.shawlaf.varlight.persistence.PersistentLightSource;
import me.shawlaf.varlight.persistence.RegionPersistor;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.RegionCoords;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;

public class VarLightCommandDebug extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandDebug(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getSyntax() {
        return "debug list (-r [regionX] [regionZ])|(-c [chunkX] [chunkZ])";
    }

    @Override
    public String getName() {
        return "debug";
    }


    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {

        VarLightCommand.assertPermission(sender, "varlight.debug");

        /*
            /vl debug list (-r [regionX] [regionZ])|(-c [chunkX] [chunkZ])
         */

        if (args.hasNext() && "gc".equalsIgnoreCase(args.peek())) {
            VarLightCommand.assertPermission(sender, "varlight.admin");

            VarLightCommand.sendPrefixedMessage(sender, "Scheduling a gc");

            new BukkitRunnable() {
                @Override
                public void run() {
                    Runtime.getRuntime().gc();
                }
            }.runTask(plugin);

            return true;
        }

        if (args.hasNext() && "list".equalsIgnoreCase(args.peek())) {
            args.next(); // Consume the peeked "list"

            if (!(sender instanceof Entity)) {
                VarLightCommand.sendPrefixedMessage(sender, "You may not run this command!");
                return true;
            }

            VarLightCommand.assertPermission(sender, "varlight.admin");

            List<PersistentLightSource> all;

            switch (args.next().toLowerCase()) {
                case "-r": {
                    int regionX, regionZ;

                    if (args.hasParameters(2)) {
                        regionX = args.nextInt();
                        regionZ = args.nextInt();
                    } else {
                        Location location = ((Entity) sender).getLocation();

                        regionX = (location.getBlockX() >> 4) >> 5;
                        regionZ = (location.getBlockZ() >> 4) >> 5;
                    }

                    WorldLightSourceManager manager = plugin.getManager(((Entity) sender).getWorld());

                    if (manager == null) {
                        VarLightCommand.sendPrefixedMessage(sender, "VarLight is not active in your current world!");
                        return true;
                    }

                    RegionPersistor<PersistentLightSource> regionPersistor = manager.getRegionPersistor(new RegionCoords(regionX, regionZ));

                    try {
                        all = regionPersistor.loadAll();
                    } catch (IOException e) {
                        throw new VarLightCommandException("Failed to load light sources", e);
                    }

                    sender.sendMessage(String.format("Light sources in region (%d | %d): [%d]", regionX, regionZ, all.size()));
                    break;
                }

                case "-c": {

                    final int chunkX, chunkZ;

                    if (args.hasParameters(2)) {
                        chunkX = args.nextInt();
                        chunkZ = args.nextInt();
                    } else {
                        Location location = ((Entity) sender).getLocation();

                        chunkX = location.getBlockX() >> 4;
                        chunkZ = location.getBlockZ() >> 4;
                    }

                    final ChunkCoords chunkCoords = new ChunkCoords(chunkX, chunkZ);

                    WorldLightSourceManager manager = plugin.getManager(((Entity) sender).getWorld());

                    if (manager == null) {
                        VarLightCommand.sendPrefixedMessage(sender, "VarLight is not active in your current world!");
                        return true;
                    }


                    final RegionPersistor<PersistentLightSource> regionPersistor = manager.getRegionPersistor(chunkCoords.toRegionCoords());

                    if (!regionPersistor.isChunkLoaded(chunkCoords)) {
                        try {
                            regionPersistor.loadChunk(chunkCoords);
                        } catch (IOException e) {
                            throw new VarLightCommandException("Failed to load light sources", e);
                        }
                    }

                    all = regionPersistor.getCache(chunkCoords);

                    sender.sendMessage(String.format("Light sources in chunk (%d | %d): [%d]", chunkX, chunkZ, all.size()));
                    break;
                }

                default: {
                    return false;
                }
            }

            for (PersistentLightSource lightSource : all) {

                TextComponent textComponent = new TextComponent(
                        String.format("    (%d | %d | %d): type: %s light: %d migrated: %s",
                                lightSource.getPosition().x,
                                lightSource.getPosition().y,
                                lightSource.getPosition().z,
                                lightSource.getType().name(),
                                lightSource.getEmittingLight(),
                                lightSource.isMigrated() ? "yes" : "no"
                        )
                );

                textComponent.setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        String.format("/tp @s %d %d %d", lightSource.getPosition().x, lightSource.getPosition().y, lightSource.getPosition().z)
                ));

                textComponent.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent("Click to teleport")
                        }
                ));

                sender.spigot().sendMessage(textComponent);
            }

        }


//        VarLightCommand.assertPermission(sender, "varlight.admin");
//
//        VarLightPlugin.DEBUG = !VarLightPlugin.DEBUG;
//        VarLightCommand.broadcastResult(sender, String.format("Updated Varlight debug state to: %s", VarLightPlugin.DEBUG), "varlight.admin");

        return true;
    }

    @Override
    public void tabComplete(CommandSuggestions commandSuggestions) {
        if (!(commandSuggestions.getCommandSender() instanceof Entity)) {
            return;
        }

        Entity sender = (Entity) commandSuggestions.getCommandSender();

        if (commandSuggestions.getArgumentCount() >= 2 && "list".equalsIgnoreCase(commandSuggestions.getArgs()[1])) {
            switch (commandSuggestions.getArgumentCount()) {
                case 2: {
                    commandSuggestions.suggestChoices("-r", "-c");
                    return;
                }

                case 3: {
                    suggestCoordinate(commandSuggestions, sender.getLocation().getBlockX() >> 4);
                    return;
                }

                case 4: {
                    suggestCoordinate(commandSuggestions, sender.getLocation().getBlockZ() >> 4);
                }
            }
        }
    }

    private void suggestCoordinate(CommandSuggestions commandSuggestions, int chunkCoordinate) {
        switch (commandSuggestions.getArgs()[1]) {
            case "-r": {
                commandSuggestions.addSuggestion(String.valueOf(chunkCoordinate >> 5));
                return;
            }

            case "-c": {
                commandSuggestions.addSuggestion(String.valueOf(chunkCoordinate));
                return;
            }
        }
    }
}
