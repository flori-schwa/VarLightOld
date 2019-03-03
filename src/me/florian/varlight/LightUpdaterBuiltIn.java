package me.florian.varlight;

import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;

import java.lang.reflect.Field;
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

    private Field nField;

    public LightUpdaterBuiltIn() {
        try {
            nField = net.minecraft.server.v1_13_R2.Block.class.getDeclaredField("n");
            nField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isBlockTransparent(Block block) {
        try {
            return ! nField.getBoolean(getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public void setLight(Location location, int lightLevel) {
        WorldServer world = getNmsWorld(location.getWorld());
        BlockPosition updateAt = toBlockPosition(location);
        Block block = location.getBlock();


        world.c(EnumSkyBlock.BLOCK, updateAt);

        if (lightLevel > 0) {
            world.a(EnumSkyBlock.BLOCK, updateAt, lightLevel);

            IntPosition intPosition = new IntPosition(block.getLocation());

            for (BlockFace blockFace : CHECK_FACES) {
                IntPosition relative = intPosition.getRelative(blockFace);

                if (relative.outOfBounds()) {
                    continue;
                }


                if (isBlockTransparent(relative.toBlock(location.getWorld()))) {
                    world.c(EnumSkyBlock.BLOCK, relative.toBlockPosition());
                    break;
                }
            }
        }


        int chunkX = location.getBlockX() / 16;
        int chunkZ = location.getBlockZ() / 16;

        List<ChunkCoordIntPair> chunksToUpdate = new ArrayList<>();

        for (int dx = - 1; dx <= 1; dx++) {
            for (int dz = - 1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;

                if (! world.getChunkProvider().isLoaded(x, z)) {
                    continue;
                }

                chunksToUpdate.add(new ChunkCoordIntPair(x, z));
            }
        }

        PlayerChunkMap playerChunkMap = world.getPlayerChunkMap();

        for (ChunkCoordIntPair chunkCoordIntPair : chunksToUpdate) {
            PlayerChunk playerChunk = playerChunkMap.getChunk(chunkCoordIntPair.x, chunkCoordIntPair.z);

            for (EntityPlayer entityPlayer : playerChunk.players) {
                entityPlayer.playerConnection.sendPacket(new PacketPlayOutMapChunk(playerChunk.chunk, (1 << 17) - 1));
            }
        }
    }
}
