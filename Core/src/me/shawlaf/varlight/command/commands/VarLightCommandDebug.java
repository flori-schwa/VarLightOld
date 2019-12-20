package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.florian.command.exception.CommandException;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import me.shawlaf.varlight.persistence.PersistentLightSource;
import me.shawlaf.varlight.persistence.RegionPersistor;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.RegionCoords;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

@SuppressWarnings("DuplicatedCode")
public class VarLightCommandDebug extends VarLightSubCommand {

    public VarLightCommandDebug(VarLightPlugin plugin) {
        super(plugin, "varlight-debug", false);
    }

    @Override
    public String getSubCommandName() {
        return "debug";
    }

    @Override
    public String getRequiredPermission() {
        return "varlight.admin.debug";
    }

    @Override
    public String getDescription() {
        return "Lists all custom Light sources in a region or chunk";
    }

    @Override
    public String getSyntax() {
        return "-r|-c [regionX|chunkX] [regionZ|chunkZ]";
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {
        return literalArgumentBuilder.then(
                LiteralArgumentBuilder.<CommandSender>literal("list")
                        .requires(sender -> sender instanceof Player)
                        .then(
                                LiteralArgumentBuilder.<CommandSender>literal("-r")
                                        .executes(context -> {
                                            if (!(context.getSource() instanceof Player)) {
                                                throw CommandException.severeException("You must be a player to use this command!"); // Technically impossible
                                            }

                                            Player player = (Player) context.getSource();

                                            int regionX = player.getLocation().getBlockX() >> 4 >> 5;
                                            int regionZ = player.getLocation().getBlockZ() >> 4 >> 5;

                                            listLightSourcesInRegion(player, regionX, regionZ);

                                            return 0;
                                        })
                                        .then(
                                                RequiredArgumentBuilder.<CommandSender, Integer>argument("regionX", integer())
                                                        .then(
                                                                RequiredArgumentBuilder.<CommandSender, Integer>argument("regionZ", integer())
                                                                        .executes(context -> {
                                                                            if (!(context.getSource() instanceof Player)) {
                                                                                throw CommandException.severeException("You must be a player to use this command!"); // Technically impossible
                                                                            }

                                                                            Player player = (Player) context.getSource();

                                                                            int regionX = context.getArgument("regionX", int.class);
                                                                            int regionZ = context.getArgument("regionZ", int.class);

                                                                            listLightSourcesInRegion(player, regionX, regionZ);

                                                                            return 0;
                                                                        })
                                                        )
                                        )
                        ).then(
                        LiteralArgumentBuilder.<CommandSender>literal("-c")
                                .executes(context -> {
                                    if (!(context.getSource() instanceof Player)) {
                                        throw CommandException.severeException("You must be a player to use this command!"); // Technically impossible
                                    }

                                    Player player = (Player) context.getSource();

                                    int chunkX = player.getLocation().getBlockX() >> 4;
                                    int chunkZ = player.getLocation().getBlockZ() >> 4;

                                    listLightSourcesInChunk(player, chunkX, chunkZ);

                                    return 0;
                                })
                                .then(
                                        RequiredArgumentBuilder.<CommandSender, Integer>argument("chunkX", integer())
                                                .then(
                                                        RequiredArgumentBuilder.<CommandSender, Integer>argument("chunkZ", integer())
                                                                .executes(context -> {
                                                                    if (!(context.getSource() instanceof Player)) {
                                                                        throw CommandException.severeException("You must be a player to use this command!"); // Technically impossible
                                                                    }

                                                                    Player player = (Player) context.getSource();

                                                                    int chunkX = context.getArgument("chunkX", int.class);
                                                                    int chunkZ = context.getArgument("chunkZ", int.class);

                                                                    listLightSourcesInChunk(player, chunkX, chunkZ);

                                                                    return 0;
                                                                })
                                                )
                                )
                )
        );
    }


    private void listLightSourcesInRegion(Player player, int regionX, int regionZ) {
        WorldLightSourceManager manager = plugin.getManager(player.getWorld());

        if (manager == null) {
            success("Varlight is not active in your current world!").finish(player);
            return;
        }

        RegionPersistor<PersistentLightSource> persistor = manager.getRegionPersistor(new RegionCoords(regionX, regionZ));
        List<PersistentLightSource> all;

        try {
            all = persistor.loadAll();
        } catch (IOException e) {
            throw CommandException.severeException("Failed to load light sources!", e);
        }

        player.sendMessage(String.format("Light sources in region (%d | %d): [%d]", regionX, regionZ, all.size()));
        listInternal(player, all);
    }

    private void listLightSourcesInChunk(Player player, int chunkX, int chunkZ) {
        WorldLightSourceManager manager = plugin.getManager(player.getWorld());

        if (manager == null) {
            success("Varlight is not active in your current world!").finish(player);
            return;
        }

        ChunkCoords chunkCoords = new ChunkCoords(chunkX, chunkZ);

        RegionPersistor<PersistentLightSource> persistor = manager.getRegionPersistor(chunkCoords.toRegionCoords());
        List<PersistentLightSource> all;

        if (!persistor.isChunkLoaded(chunkCoords)) {
            try {
                persistor.loadChunk(chunkCoords);
            } catch (IOException e) {
                throw CommandException.severeException("Failed to load light sources!", e);
            }
        }

        all = persistor.getCache(chunkCoords);

        player.sendMessage(String.format("Light sources in chunk (%d | %d): [%d]", chunkX, chunkZ, all.size()));
        listInternal(player, all);
    }

    private void listInternal(Player player, List<PersistentLightSource> list) {
        for (PersistentLightSource lightSource : list) {

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

            player.spigot().sendMessage(textComponent);
        }
    }
}
