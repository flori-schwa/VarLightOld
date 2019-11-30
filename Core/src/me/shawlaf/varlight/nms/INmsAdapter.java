package me.shawlaf.varlight.nms;

import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface INmsAdapter {

    default void onLoad() {

    }

    default void onEnable() {

    }

    default void onDisable() {

    }

    default void onWorldEnable(@NotNull World world) {

    }

    boolean isBlockTransparent(@NotNull Block block);

    void updateBlockLight(@NotNull Location at, int lightLevel);

    int getEmittingLightLevel(@NotNull Block block);

    void sendChunkUpdates(@NotNull Chunk chunk, int mask);

    default void sendChunkUpdates(@NotNull Chunk chunk) {
        sendChunkUpdates(chunk, (1 << 16) - 1);
    }

    boolean isIllegalBlock(@NotNull Block block);

    void sendActionBarMessage(Player player, String message);

    @Nullable
    Block getTargetBlockExact(Player player, int maxDistance);

    @NotNull
    String getNumericMinecraftVersion();

    @NotNull
    default NumericMajorMinorVersion getMinecraftVersion() {
        return new NumericMajorMinorVersion(getNumericMinecraftVersion());
    }

    default void suggestCommand(Player player, String command) {
        player.spigot().sendMessage(
                new ComponentBuilder(String.format("Click to here to run command %s", command))
                        .color(ChatColor.GRAY)
                        .italic(true)
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))

                        .event(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("You can thank MC-70317").color(ChatColor.GRAY).italic(true).create())
                        )
                        .create()
        );
    }

    default int getChunkBitMask(@NotNull Location location) {
        Objects.requireNonNull(location);

        return getChunkBitMask(location.getBlockY() / 16);
    }

    default int getChunkBitMask(int sectionY) {
        int mask = 1 << sectionY;

        if (sectionY == 0) {
            return mask | 2;
        }

        if (sectionY == 15) {
            return mask | 0x4000;
        }

        return mask | (1 << (sectionY - 1)) | (1 << (sectionY + 1));
    }

    @NotNull
    default List<Chunk> collectChunksToUpdate(@NotNull Location location) {
        Objects.requireNonNull(location);

        return collectChunksToUpdate(new IntPosition(location), location.getWorld());
    }

    @NotNull
    default List<Chunk> collectChunksToUpdate(@NotNull IntPosition location, @NotNull World world) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(world);

        int chunkX = location.getChunkX();
        int chunkZ = location.getChunkZ();

        List<Chunk> chunksToUpdate = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;

                if (!world.isChunkLoaded(x, z)) {
                    continue;
                }

                chunksToUpdate.add(world.getChunkAt(x, z));
            }
        }
        return chunksToUpdate;
    }
}
