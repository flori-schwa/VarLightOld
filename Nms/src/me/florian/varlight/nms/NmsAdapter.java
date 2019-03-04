package me.florian.varlight.nms;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

public interface NmsAdapter {

    boolean isBlockTransparent(Block block);

    void recalculateBlockLight(Location at);

    void updateBlockLight(Location at, int lightLevel);

    int getEmittingLightLevel(Block block);

    void sendChunkUpdates(Chunk chunk);

    boolean isValidBlock(Block block);
}
