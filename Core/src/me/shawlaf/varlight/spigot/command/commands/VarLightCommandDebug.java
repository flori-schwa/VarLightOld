package me.shawlaf.varlight.spigot.command.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.Paginator;
import me.shawlaf.varlight.util.RegionCoords;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.ToIntFunction;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static me.shawlaf.command.result.CommandResult.*;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

@SuppressWarnings("DuplicatedCode")
public class VarLightCommandDebug extends VarLightSubCommand {

    private static final int PAGE_SIZE = 10;

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_REGION_X = argument("regionX", integer());
    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_REGION_Z = argument("regionZ", integer());

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_CHUNK_X = argument("regionX", integer());
    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_CHUNK_Z = argument("regionZ", integer());

    public VarLightCommandDebug(VarLightPlugin plugin) {
        super(plugin, "debug");
    }

    @NotNull
    @Override
    public String getRequiredPermission() {
        return "varlight.admin.debug";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Lists all custom Light sources in a region or chunk";
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " -r|-c [regionX|chunkX] [regionZ|chunkZ]";
    }

    private void suggestCoordinate(RequiredArgumentBuilder<CommandSender, Integer> coordinateArgument, ToIntFunction<Entity> coordinateSupplier) {
        coordinateArgument.suggests(((context, builder) -> {
            if (!(context.getSource() instanceof Entity)) {
                return builder.buildFuture();
            }

            builder.suggest(coordinateSupplier.applyAsInt((Entity) context.getSource()));

            return builder.buildFuture();
        }));
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {

        suggestCoordinate(ARG_CHUNK_X, e -> e.getLocation().getBlockX() >> 4);
        suggestCoordinate(ARG_CHUNK_Z, e -> e.getLocation().getBlockZ() >> 4);

        suggestCoordinate(ARG_REGION_X, e -> (e.getLocation().getBlockX() >> 4) >> 5);
        suggestCoordinate(ARG_REGION_Z, e -> (e.getLocation().getBlockZ() >> 4) >> 5);

        literalArgumentBuilder.then(
                LiteralArgumentBuilder.<CommandSender>literal("list")
                        .then(
                                LiteralArgumentBuilder.<CommandSender>literal("-r")
                                        .executes(this::regionImplicit)
                                        .then(
                                                ARG_REGION_X.then(ARG_REGION_Z.executes((c) -> regionExplicit(c, 1))
                                                        .then(
                                                                RequiredArgumentBuilder.<CommandSender, Integer>argument("rpage", integer(1))
                                                                        .executes(
                                                                                c -> regionExplicit(c, c.getArgument("rpage", int.class))
                                                                        )
                                                        )
                                                )
                                        )
                        ).then(
                        LiteralArgumentBuilder.<CommandSender>literal("-c")
                                .executes(this::chunkImplicit)
                                .then(
                                        ARG_CHUNK_X.then(ARG_CHUNK_Z.executes(c -> chunkExplicit(c, 1))
                                                .then(
                                                        RequiredArgumentBuilder.<CommandSender, Integer>argument("cpage", integer(1))
                                                                .executes(
                                                                        c -> chunkExplicit(c, c.getArgument("cpage", int.class))
                                                                )
                                                )
                                        )
                                )
                )
        );

        literalArgumentBuilder.then(
                LiteralArgumentBuilder.<CommandSender>literal("stick").executes(context -> {
                    if (!(context.getSource() instanceof Player)) {
                        failure(this, context.getSource(), "You must be a player to use this command!");
                        return FAILURE;
                    }

                    ((Player) context.getSource()).getInventory().addItem(plugin.getNmsAdapter().getVarLightDebugStick());

                    return SUCCESS;
                })
        );

        literalArgumentBuilder.then(
                LiteralArgumentBuilder.<CommandSender>literal("logger")
                        .requires(cs -> cs.hasPermission("varlight.admin"))
                        .then(
                                RequiredArgumentBuilder.<CommandSender, Boolean>argument("value", bool())
                                        .executes(c -> {
                                            boolean newStatus = BoolArgumentType.getBool(c, "value");
                                            plugin.getDebugManager().setDebugEnabled(newStatus);

                                            successBroadcast(this, c.getSource(), (newStatus ? "enabled" : "disabled") + " debug logging.", "varlight.admin");

                                            return newStatus ? 1 : 0;
                                        })
                        )
        );

        return literalArgumentBuilder;
    }

    private int regionImplicit(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int regionX = player.getLocation().getBlockX() >> 4 >> 5;
        int regionZ = player.getLocation().getBlockZ() >> 4 >> 5;

        listLightSourcesInRegion(player, regionX, regionZ, 1);

        return SUCCESS;
    }

    private int regionExplicit(CommandContext<CommandSender> context, int page) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int regionX = context.getArgument(ARG_REGION_X.getName(), int.class);
        int regionZ = context.getArgument(ARG_REGION_Z.getName(), int.class);

        listLightSourcesInRegion(player, regionX, regionZ, page);

        return SUCCESS;
    }

    private int chunkImplicit(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int chunkX = player.getLocation().getBlockX() >> 4;
        int chunkZ = player.getLocation().getBlockZ() >> 4;

        listLightSourcesInChunk(player, chunkX, chunkZ, 1);

        return SUCCESS;
    }

    private int chunkExplicit(CommandContext<CommandSender> context, int page) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int chunkX = context.getArgument(ARG_CHUNK_X.getName(), int.class);
        int chunkZ = context.getArgument(ARG_CHUNK_Z.getName(), int.class);

        listLightSourcesInChunk(player, chunkX, chunkZ, page);

        return SUCCESS;
    }

    private void listLightSourcesInRegion(Player player, int regionX, int regionZ, int page) {
        WorldLightSourceManager manager = plugin.getManager(player.getWorld());

        if (manager == null) {
            success(this, player, "Varlight is not active in your current world!");
            return;
        }

        List<IntPosition> all = manager.getNLSFile(new RegionCoords(regionX, regionZ)).getAllLightSources();

        int pages = Paginator.getAmountPages(all.size(), PAGE_SIZE);
        page = Math.min(page, pages);

        List<IntPosition> pageList = Paginator.paginateEntries(all, PAGE_SIZE, page);

        player.sendMessage(String.format("Light sources in region (%d | %d): %d (Showing Page %d / %d)", regionX, regionZ, all.size(), page, pages));
        listInternal(player, pageList);
    }

    private void listLightSourcesInChunk(Player player, int chunkX, int chunkZ, int page) {
        WorldLightSourceManager manager = plugin.getManager(player.getWorld());

        if (manager == null) {
            success(this, player, "Varlight is not active in your current world!");
            return;
        }

        ChunkCoords chunkCoords = new ChunkCoords(chunkX, chunkZ);

        List<IntPosition> all = manager.getNLSFile(chunkCoords.toRegionCoords()).getAllLightSources(chunkCoords);

        int pages = Paginator.getAmountPages(all.size(), PAGE_SIZE);
        page = Math.min(page, pages);

        List<IntPosition> pageList = Paginator.paginateEntries(all, PAGE_SIZE, page);

        player.sendMessage(String.format("Light sources in chunk (%d | %d): %d (Showing Page %d / %d)", chunkX, chunkZ, all.size(), page, pages));
        listInternal(player, pageList);
    }

    @SuppressWarnings("ConstantConditions")
    // All Methods that call this Method already assert that the WorldLightSourceManager exists!
    private void listInternal(Player player, List<IntPosition> list) {
        WorldLightSourceManager manager = plugin.getManager(player.getWorld());

        for (IntPosition lightSource : list) {
            TextComponent textComponent = new TextComponent(String.format(
                    "%s = %d (%s)",
                    lightSource.toShortString(),
                    manager.getCustomLuminance(lightSource, 0),
                    player.getWorld().getBlockAt(lightSource.x, lightSource.y, lightSource.z).getType().getKey().toString())
            );

            textComponent.setColor(ChatColor.GREEN);

            textComponent.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    String.format("/tp %d %d %d", lightSource.x, lightSource.y, lightSource.z)
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
