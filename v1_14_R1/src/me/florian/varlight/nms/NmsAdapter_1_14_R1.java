package me.florian.varlight.nms;


import me.florian.varlight.nms.persistence.LightSourcePersistor;
import me.florian.varlight.nms.persistence.PersistentLightSource;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.material.Openable;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NmsAdapter_1_14_R1 implements NmsAdapter {

    private static final BlockFace[] CHECK_FACES = new BlockFace[] {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH
    };

    private static final Field LIGHT_BLOCKING_FIELD, LIGHT_ENGINE_FIELD_CHUNK_MAP, LIGHT_ENGINE_FIELD_CHUNK_PROVIDER, FIELD_LIGHT_ENGINE_LAYER_STORAGE, LIGHT_ENGINE_THREADED_MAILBOX_FIELD;
    private static final Method LIGHT_ENGINE_STORAGE_WRITE_DATA_METHOD, METHOD_LIGHT_ENGINE_STORAGE_GET_NIBBLE_ARRAY, METHOD_LIGHT_ENGINE_LAYER_CHECK_BLOCK;
    public static final String TAG_METADATA_KEY = "varlight:tagged";

    static {
        try {
            LIGHT_BLOCKING_FIELD = ReflectionHelper.Safe.getField(net.minecraft.server.v1_14_R1.Block.class, "v");

            LIGHT_ENGINE_FIELD_CHUNK_MAP = ReflectionHelper.Safe.getField(PlayerChunkMap.class, "lightEngine");
            LIGHT_ENGINE_FIELD_CHUNK_PROVIDER = ReflectionHelper.Safe.getField(ChunkProviderServer.class, "lightEngine");

            LIGHT_ENGINE_THREADED_MAILBOX_FIELD = ReflectionHelper.Safe.getField(LightEngineThreaded.class, "e");

            FIELD_LIGHT_ENGINE_LAYER_STORAGE = ReflectionHelper.Safe.getField(LightEngineLayer.class, "c");

            LIGHT_ENGINE_STORAGE_WRITE_DATA_METHOD = ReflectionHelper.Safe.getMethod(LightEngineStorage.class, "b", long.class, int.class);

            METHOD_LIGHT_ENGINE_STORAGE_GET_NIBBLE_ARRAY = ReflectionHelper.Safe.getMethod(LightEngineStorage.class, "a", long.class, boolean.class);
            METHOD_LIGHT_ENGINE_LAYER_CHECK_BLOCK = ReflectionHelper.Safe.getMethod(LightEngineLayer.class, "f", long.class);

        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new NmsInitializationException(e);
        }
    }

    private static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("VarLight");
    }

    private LightEngineThreaded getLightEngine(World world) {
        return getNmsWorld(world).getChunkProvider().getLightEngine();
    }

    private LightEngineLayer<?, ?> getLightEngineBlock(LightEngine lightEngine) {
        return (LightEngineLayer<?, ?>) lightEngine.a(EnumSkyBlock.BLOCK);
    }

    private boolean isCustomLightEngineInjected(WorldServer worldServer) {
        try {
            return ReflectionHelper.Safe.get(LIGHT_ENGINE_FIELD_CHUNK_MAP, worldServer.getChunkProvider().playerChunkMap) instanceof CustomLightEngine
                    && worldServer.getChunkProvider().getLightEngine() instanceof CustomLightEngine;
        } catch (IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    private void injectCustomLightEngine(WorldServer worldServer) throws IllegalAccessException {
        if (isCustomLightEngineInjected(worldServer)) {
            return;
        }

        LightEngineThreaded base = worldServer.getChunkProvider().getLightEngine();
        CustomLightEngine customLightEngine = new CustomLightEngine(base, worldServer);

        ReflectionHelper.Safe.set(worldServer.getChunkProvider(), LIGHT_ENGINE_FIELD_CHUNK_PROVIDER, customLightEngine);
        ReflectionHelper.Safe.set(worldServer.getChunkProvider().playerChunkMap, LIGHT_ENGINE_FIELD_CHUNK_MAP, customLightEngine);

        System.out.println("Injected Custom Lighting engine in world " + worldServer.getWorldData().getName());
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
            return ! (boolean) ReflectionHelper.Safe.get(LIGHT_BLOCKING_FIELD, getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock());
        } catch (IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    protected static int getBrightness(WorldServer world, BlockPosition blockPosition) {
        return Math.max(LightSourcePersistor
                .getPersistor(getPlugin(), world.getWorld())
                .getPersistentLightSource(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ())
                .map(PersistentLightSource::getEmittingLight)
                .orElse((byte) 0), world.getType(blockPosition).h());
    }

    @Override
    public void recalculateBlockLight(Location at) {
//        LightEngineThreaded lightEngine = getLightEngine(at.getWorld());

    }

    @Override
    public void updateBlockLight(Location at, int lightLevel) {

        WorldServer worldServer = getNmsWorld(at.getWorld());
        BlockPosition blockPosition = toBlockPosition(at);

//        net.minecraft.server.v1_14_R1.Chunk chunk = worldServer.getChunkAtWorldCoords(blockPosition);
//
//        System.out.println(worldServer.getChunkAtWorldCoords(blockPosition).a(blockPosition, lightLevel, false));

        try {
            injectCustomLightEngine(worldServer);
        } catch (IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }

        LightEngineThreaded lightEngine = getLightEngine(at.getWorld());
        LightEngineLayer<?, ?> lightEngineLayer = getLightEngineBlock(lightEngine);

        lightEngineLayer.a(blockPosition, lightLevel); // Write light level to Block Light Engine
//        getNibbleArrayStorage(lightEngineLayer, blockPosition, true).a(blockPosition.getX() & 0xF, blockPosition.getY() & 0xF, blockPosition.getZ() & 0xF, lightLevel);
//
//        lightEngine.a(blockPosition);

//        for (EnumDirection direction : EnumDirection.values()) {
//            checkBlock(lightEngineLayer, blockPosition.shift(direction));
//        }

        lightEngine.a(worldServer.getChunkAtWorldCoords(blockPosition), true);

//        worldServer.getMinecraftServer().awaitTasks(lightEngine.a(worldServer.getChunkAtWorldCoords(blockPosition), true)::isDone);

//        lightEngine.queueUpdate();
//
//        LazyMetadataValue metadataValue = new LazyMetadataValue(getPlugin(), LazyMetadataValue.CacheStrategy.CACHE_ETERNALLY, () -> at.getBlock().getType());
//        metadataValue.value(); // Cache the block type
//        at.getBlock().setMetadata(TAG_METADATA_KEY, metadataValue);
//        playerChunkMap
//                .a(
//                        playerChunkMap.visibleChunks.get(new ChunkCoordIntPair(blockPosition).pair()),
//                        ChunkStatus.LIGHT); // Run Task "lightChunk"
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
