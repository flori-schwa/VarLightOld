package me.shawlaf.varlight.command_old.commands;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command_old.ArgumentIterator;
import me.shawlaf.varlight.command_old.CommandSuggestions;
import me.shawlaf.varlight.command_old.VarLightCommand;
import me.shawlaf.varlight.command_old.VarLightSubCommand;
import me.shawlaf.varlight.command_old.exception.VarLightCommandException;
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
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

@Deprecated
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

        if (args.hasParameters(2) && "list".equalsIgnoreCase(args.peek())) {
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
        if (!(commandSuggestions.getCommandSender() instanceof Player)) {
            return;
        }

        if (commandSuggestions.getArgumentCount() == 1) {
            commandSuggestions.suggestChoices("list");

            return;
        }

        if (commandSuggestions.getArgumentCount() >= 2 && "list".equalsIgnoreCase(commandSuggestions.getArgs()[0])) {
            switch (commandSuggestions.getArgumentCount()) {
                case 2: {
                    commandSuggestions.suggestChoices("-r", "-c");
                    return;
                }

                case 3:
                case 4: {
                    switch (commandSuggestions.getArgs()[1]) {
                        case "-r": {
                            commandSuggestions.suggestRegionPosition(commandSuggestions.getArgumentCount() - 3, true);
                            return;
                        }

                        case "-c": {
                            commandSuggestions.suggestChunkPosition(commandSuggestions.getArgumentCount() - 3, true);
                        }
                    }
                }
            }
        }
    }
}
