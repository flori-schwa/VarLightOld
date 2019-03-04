package me.florian.varlight;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

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

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
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
                    break;
                }
            }
        }


        int chunkX = location.getBlockX() / 16;
        int chunkZ = location.getBlockZ() / 16;

        List<Chunk> chunksToUpdate = new ArrayList<>();

        for (int dx = - 1; dx <= 1; dx++) {
            for (int dz = - 1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;

                if (! world.isChunkLoaded(x, z)) {
                    continue;
                }

                chunksToUpdate.add(world.getChunkAt(x, z));
            }
        }

        for (Chunk chunk : chunksToUpdate) {
            varLightPlugin.getNmsAdapter().sendChunkUpdates(chunk);
        }
    }
}
