package me.florian.varlight.nms;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.Redstone;

public class NmsAdapter_1_12_R1 implements NmsAdapter {


    private Class[] blacklistedDatas = new Class[] {
            Redstone.class,
            DirectionalContainer.class,
            PistonBaseMaterial.class
    };

    public NmsAdapter_1_12_R1() {
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean isBlockTransparent(Block block) {
        return ! getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getMaterial().blocksLight();
    }

    @Override
    public void recalculateBlockLight(Location at) {
        getNmsWorld(at.getWorld()).c(EnumSkyBlock.BLOCK, toBlockPosition(at));
    }

    @Override
    public void updateBlockLight(Location at, int lightLevel) {
        getNmsWorld(at.getWorld()).a(EnumSkyBlock.BLOCK, toBlockPosition(at), lightLevel);
    }

    @Override
    public int getEmittingLightLevel(Block block) {
        return ((CraftWorld) block.getWorld()).getHandle().getChunkAt(block.getChunk().getX(), block.getChunk().getZ()).getBlockData(toBlockPosition(block.getLocation())).d();
    }

    @Override
    public void sendChunkUpdates(Chunk chunk) {
        WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
        PlayerChunkMap playerChunkMap = nmsWorld.getPlayerChunkMap();
        PlayerChunk playerChunk = playerChunkMap.getChunk(chunk.getX(), chunk.getZ());

        playerChunk.a(new PacketPlayOutMapChunk(playerChunk.chunk, (1 << 17) - 1));
    }

    @Override
    public boolean isValidBlock(Block block) {
        if (! block.getType().isBlock()) {
            return false;
        }

        if (getEmittingLightLevel(block) > 0) {
            return false;
        }

        Class<? extends MaterialData> data = block.getType().getData();

        for (Class blacklisted : blacklistedDatas) {
            if (blacklisted.isAssignableFrom(data)) {
                return false;
            }
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return false;
        }

        return block.getType().isSolid() && block.getType().isOccluding();
    }
}
