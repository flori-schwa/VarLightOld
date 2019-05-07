package me.florian.varlight.nms;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public interface NmsAdapter extends Listener {

    boolean isBlockTransparent(Block block);

    void recalculateBlockLight(Location at);

    void updateBlockLight(Location at, int lightLevel);

    int getEmittingLightLevel(Block block);

    void sendChunkUpdates(Chunk chunk, int mask);

    default void sendChunkUpdates(Chunk chunk) {
        sendChunkUpdates(chunk, (1 << 16) - 1);
    }

    boolean isValidBlock(Block block);

    void sendActionBarMessage(Player player, String message);
}
