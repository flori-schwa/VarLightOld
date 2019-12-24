package me.shawlaf.varlight.nms;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.util.IntPosition;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.Redstone;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;

@ForMinecraft(version = "1.8.8")
public class NmsAdapter implements INmsAdapter {

    private static final BlockFace[] CHECK_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private static final byte GAME_INFO = 2;
    private final Class[] blacklistedDatas = new Class[]{
            Redstone.class,
            DirectionalContainer.class,
            PistonBaseMaterial.class
    };
    private Field longHashMapPlayerChunk;
    private Method broadcastPacket;

    @SuppressWarnings("unchecked")
    public NmsAdapter(VarLightPlugin plugin) {
        try {
            Class playerChunkClass = Class.forName("net.minecraft.server.v1_8_R3.PlayerChunkMap$PlayerChunk");

            longHashMapPlayerChunk = PlayerChunkMap.class.getDeclaredField("d");
            longHashMapPlayerChunk.setAccessible(true);

            broadcastPacket = playerChunkClass.getDeclaredMethod("a", Packet.class);
            broadcastPacket.setAccessible(true);
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    private Object getPlayerChunk(Chunk chunk) throws IllegalAccessException {
        long encoded = (long) chunk.getX() + 2147483647L | (long) chunk.getZ() + 2147483647L << 32;
        return ((LongHashMap) longHashMapPlayerChunk.get(getNmsWorld(chunk.getWorld()).getPlayerChunkMap())).getEntry(encoded);
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean isInvalidLightUpdateItem(Material material) {
        return material.isBlock();
    }

    @Override
    public boolean isBlockTransparent(@NotNull Block block) {
        return !getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock().getMaterial().blocksLight();
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
        return getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock().r(); // TODO VERIFY
    }

    @Override
    public void sendChunkUpdates(@NotNull Chunk chunk, int mask) {
        try {
            WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
            broadcastPacket.invoke(getPlayerChunk(chunk), new PacketPlayOutMapChunk(nmsWorld.getChunkAt(chunk.getX(), chunk.getZ()), false, mask));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
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

    @Override
    public void sendActionBarMessage(Player player, String message) {
        TextComponent textComponent = new TextComponent(message);
        PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(null, GAME_INFO);
        packetPlayOutChat.components = new BaseComponent[]{textComponent};

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutChat);
    }

    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

    @Override
    public Block getTargetBlockExact(Player player, int maxDistance) {
        return player.getTargetBlock(new HashSet<Material>(), maxDistance);
    }
}
