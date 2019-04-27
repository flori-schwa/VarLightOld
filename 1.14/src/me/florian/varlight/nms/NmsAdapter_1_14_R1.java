package me.florian.varlight.nms;


import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.material.Openable;

import java.lang.reflect.Field;

public class NmsAdapter_1_14_R1 implements NmsAdapter {


    private Field lightBlockingField;
    private Field lightEngineLayerField;

    public NmsAdapter_1_14_R1() {
        try {
            lightBlockingField = ReflectionHelper.Safe.getField(net.minecraft.server.v1_14_R1.Block.class, "v");
            lightEngineLayerField = ReflectionHelper.Safe.getField(LightEngine.class, "a");
        } catch (NoSuchFieldException e) {
            throw new NmsInitializationException(e);
        }
    }

    private LightEngineThreaded getLightEngine(World world) {
        return getNmsWorld(world).getChunkProvider().getLightEngine();
    }

    private LightEngineLayer<?, ?> getLightEngineBlock(LightEngine lightEngine) {
        try {
            return ReflectionHelper.Safe.get(lightEngineLayerField, lightEngine);
        } catch (IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean isBlockTransparent(Block block) {
        try {
            return ! (boolean) ReflectionHelper.Safe.get(lightBlockingField, getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock());
        } catch (IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    @Override
    public void recalculateBlockLight(Location at) {
//
//        LightEngineThreaded lightEngine = getLightEngine(at.getWorld());
//
//        LightEngineLayer<?, ?> lightEngineLayer = getLightEngineBlock(lightEngine);
//        lightEngineLayer.a(toBlockPosition(at));

//        lightEngine.queueUpdate();
    }

    @Override
    public void updateBlockLight(Location at, int lightLevel) {
        LightEngineThreaded lightEngine = getLightEngine(at.getWorld());

        LightEngineLayer<?, ?> lightEngineBlock = getLightEngineBlock(lightEngine);
        lightEngineBlock.a(toBlockPosition(at), lightLevel);
    }

    @Override
    public int getEmittingLightLevel(Block block) {
        IBlockData blockData = ((CraftWorld) block.getWorld()).getHandle().getChunkAt(block.getChunk().getX(), block.getChunk().getZ()).getType(toBlockPosition(block.getLocation()));

        return blockData.getBlock().a(blockData);
    }

    @Override
    public void sendChunkUpdates(Chunk chunk, int mask) {
        WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
        PlayerChunkMap playerChunkMap = nmsWorld.getChunkProvider().playerChunkMap;
        PlayerChunk playerChunk = playerChunkMap.visibleChunks.get(ChunkCoordIntPair.pair(chunk.getX(), chunk.getZ()));

        for (int cy = 0; cy < 16; cy++) {
            if ((mask & (1 << cy)) == 0) {
                continue;
            }

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        playerChunk.a(x, cy * 16 + y, z);
                    }
                }
            }
        }

        playerChunk.a(playerChunk.getChunk());
    }

    @Override
    public boolean isValidBlock(Block block) {
        if (! block.getType().isBlock()) {
            return false;
        }

        if (getEmittingLightLevel(block) > 0) {
            return false;
        }

        BlockData blockData = block.getType().createBlockData();

        if (blockData instanceof Powerable || blockData instanceof AnaloguePowerable || blockData instanceof Openable || blockData instanceof Piston) {
            return false;
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return false;
        }

        if (block.getType() == Material.BLUE_ICE) {
            return true; // Packed ice is solid and occluding but blue ice isn't?
        }

        return block.getType().isSolid() && block.getType().isOccluding();
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
