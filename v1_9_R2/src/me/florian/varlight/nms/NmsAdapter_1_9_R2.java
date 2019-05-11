package me.florian.varlight.nms;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_9_R2.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.Redstone;

public class NmsAdapter_1_9_R2 implements NmsAdapter {

    private Class[] blacklistedDatas = new Class[] {
            Redstone.class,
            DirectionalContainer.class,
            PistonBaseMaterial.class
    };

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
        return getNmsWorld(block.getWorld()).getChunkAt(block.getChunk().getX(), block.getChunk().getZ()).getBlockData(toBlockPosition(block.getLocation())).d();
    }

    @Override
    public void sendChunkUpdates(Chunk chunk, int mask) {
        WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
        PlayerChunkMap playerChunkMap = nmsWorld.getPlayerChunkMap();
        PlayerChunk playerChunk = playerChunkMap.getChunk(chunk.getX(), chunk.getZ());

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

        playerChunk.d();
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

    @Override
    public void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    @Override
    public void setCooldown(Player player, Material material, int ticks) {
        // Ignore
    }

    @Override
    public boolean hasCooldown(Player player, Material material) {
        return false;
    }
}
