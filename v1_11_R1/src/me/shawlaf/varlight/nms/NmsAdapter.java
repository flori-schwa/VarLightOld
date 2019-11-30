package me.shawlaf.varlight.nms;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.util.IntPosition;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.Redstone;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@ForMinecraft(version = "1.11.2")
public class NmsAdapter implements INmsAdapter {

    private static final BlockFace[] CHECK_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private final Class[] blacklistedDatas = new Class[]{
            Redstone.class,
            DirectionalContainer.class,
            PistonBaseMaterial.class
    };

    public NmsAdapter(VarLightPlugin plugin) {

    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean isBlockTransparent(@NotNull Block block) {
        return !getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getMaterial().blocksLight();
    }

    private void recalculateBlockLight(Location at) {
        getNmsWorld(at.getWorld()).c(EnumSkyBlock.BLOCK, toBlockPosition(at));
    }

    @Override
    public void updateBlockLight(@NotNull Location at, int lightLevel) {
        Block block = at.getBlock();
        World world = at.getWorld();

        recalculateBlockLight(at);

        if (lightLevel > 0) {
            getNmsWorld(world).a(EnumSkyBlock.BLOCK, toBlockPosition(at), lightLevel);

            IntPosition intPosition = new IntPosition(block.getLocation());

            for (BlockFace blockFace : CHECK_FACES) {
                IntPosition relative = intPosition.getRelative(blockFace);

                if (relative.outOfBounds()) {
                    continue;
                }

                if (isBlockTransparent(relative.toBlock(world))) {
                    recalculateBlockLight(relative.toLocation(world));
                }
            }
        }

        List<Chunk> chunksToUpdate = collectChunksToUpdate(at);

        int mask = getChunkBitMask(at);

        for (Chunk chunk : chunksToUpdate) {
            sendChunkUpdates(chunk, mask);
        }
    }

    @Override
    public int getEmittingLightLevel(@NotNull Block block) {
        return getNmsWorld(block.getWorld()).getChunkAt(block.getChunk().getX(), block.getChunk().getZ()).getBlockData(toBlockPosition(block.getLocation())).d();
    }

    @Override
    public void sendChunkUpdates(@NotNull Chunk chunk, int mask) {
        WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
        PlayerChunkMap playerChunkMap = nmsWorld.getPlayerChunkMap();
        PlayerChunk playerChunk = playerChunkMap.getChunk(chunk.getX(), chunk.getZ());

        Objects.requireNonNull(playerChunk);
        Objects.requireNonNull(playerChunk.chunk);

        playerChunk.a(new PacketPlayOutMapChunk(playerChunk.chunk, mask));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isIllegalBlock(@NotNull Block block) {
        if (!block.getType().isBlock()) {
            return true;
        }

        if (getEmittingLightLevel(block) > 0) {
            return true;
        }

        Class<? extends MaterialData> data = block.getType().getData();

        for (Class blacklisted : blacklistedDatas) {
            if (blacklisted.isAssignableFrom(data)) {
                return true;
            }
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return true;
        }

        return !block.getType().isSolid() || !block.getType().isOccluding();
    }

    @SuppressWarnings("deprecation")
    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    @Override
    public Block getTargetBlockExact(Player player, int maxDistance) {
        return player.getTargetBlock(new HashSet<Material>(), maxDistance);
    }
}
