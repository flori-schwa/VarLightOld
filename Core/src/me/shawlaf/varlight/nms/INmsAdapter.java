package me.shawlaf.varlight.nms;

import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public interface INmsAdapter {

    default void onLoad() {

    }

    default void onEnable() {

    }

    default void onDisable() {

    }

    default void onWorldEnable(World world) {

    }

    default boolean isLightApiAllowed() {
        return true;
    }

    boolean isBlockTransparent(Block block);

    void updateBlockLight(Location at, int lightLevel);

    int getEmittingLightLevel(Block block);

    void sendChunkUpdates(Chunk chunk, int mask);

    default void sendChunkUpdates(Chunk chunk) {
        sendChunkUpdates(chunk, (1 << 16) - 1);
    }

    boolean isValidBlock(Block block);

    void sendActionBarMessage(Player player, String message);

    void setCooldown(Player player, Material material, int ticks);

    boolean hasCooldown(Player player, Material material);

    Block getTargetBlockExact(Player player, int maxDistance);

    String getNumericMinecraftVersion();

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

    default int getChunkBitMask(Location location) {
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

    default List<Chunk> collectChunksToUpdate(Location location) {
        return collectChunksToUpdate(new IntPosition(location), location.getWorld());
    }

    default List<Chunk> collectChunksToUpdate(IntPosition location, World world) {
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
