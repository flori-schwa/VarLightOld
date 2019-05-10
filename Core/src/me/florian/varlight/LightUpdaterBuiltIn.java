package me.florian.varlight;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class LightUpdaterBuiltIn implements LightUpdater {

    private static final BlockFace[] CHECK_FACES = new BlockFace[] {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private VarLightPlugin varLightPlugin;

    public LightUpdaterBuiltIn(VarLightPlugin varLightPlugin) {
        this.varLightPlugin = varLightPlugin;
    }


    @Override
    public void setLight(Location location, int lightLevel) {
        Block block = location.getBlock();
        World world = location.getWorld();

        varLightPlugin.getNmsAdapter().recalculateBlockLight(location);

        if (lightLevel > 0) {
            varLightPlugin.getNmsAdapter().updateBlockLight(location, lightLevel);

            IntPosition intPosition = new IntPosition(block.getLocation());

            for (BlockFace blockFace : CHECK_FACES) {
                IntPosition relative = intPosition.getRelative(blockFace);

                if (relative.outOfBounds()) {
                    continue;
                }


                if (varLightPlugin.getNmsAdapter().isBlockTransparent(relative.toBlock(world))) {
                    varLightPlugin.getNmsAdapter().recalculateBlockLight(relative.toLocation(world));
                }
            }
        }


        List<Chunk> chunksToUpdate = getChunksToUpdate(location);

        int mask = getChunkBitMask(location);

        for (Chunk chunk : chunksToUpdate) {
            varLightPlugin.getNmsAdapter().sendChunkUpdates(chunk, mask);
        }
    }

}
