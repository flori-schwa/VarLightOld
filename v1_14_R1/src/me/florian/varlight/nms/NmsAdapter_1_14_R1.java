package me.florian.varlight.nms;


import com.google.common.collect.BiMap;
import com.mojang.datafixers.util.Either;
import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.nms.persistence.LightSourcePersistor;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.material.Openable;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Optional;
import java.util.logging.Logger;

public class NmsAdapter_1_14_R1 implements NmsAdapter {

    private static final BlockFace[] CHECK_FACES = new BlockFace[] {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH
    };

    public static final Logger LOGGER = Logger.getLogger(NmsAdapter_1_14_R1.class.getName());
    private static NmsAdapter_1_14_R1 INSTANCE = null;

    private static final Field LIGHT_BLOCKING_FIELD, LIGHT_ENGINE_FIELD_CHUNK_MAP, LIGHT_ENGINE_FIELD_CHUNK_PROVIDER, FIELD_LIGHT_ENGINE_LAYER_STORAGE, LIGHT_ENGINE_THREADED_MAILBOX_FIELD, FIELD_LIGHT_ENGINE_LAYER_I_LIGHT_ACCESS;
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

            FIELD_LIGHT_ENGINE_LAYER_I_LIGHT_ACCESS = ReflectionHelper.Safe.getField(LightEngineLayer.class, "a");

        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new NmsInitializationException(e);
        }
    }

    {
        if (INSTANCE != null) {
            throw new IllegalStateException("Created second instance of Singleton Object " + getClass().getName());
        }

        INSTANCE = this;
    }

    private VarLightPlugin plugin;

    @Override
    public void onLoad(Plugin plugin, boolean use) {
        this.plugin = (VarLightPlugin) plugin;

        if (! use) {
            return;
        }

        injectCustomChunkStatus();

//        try {
//            for (World world : Bukkit.getWorlds()) {
//                injectCustomLightEngine(getNmsWorld(world));
//            }
//        } catch (IllegalAccessException e) {
//            throw new NmsInitializationException(e);
//        }
    }

    @Override
    public void onEnable(Plugin plugin, boolean use) {
        if (! use) {
            return;
        }

        try {
            for (World world : Bukkit.getWorlds()) {
                injectCustomIBlockAccess(world);
            }
        } catch (IllegalAccessException e) {
            throw new NmsInitializationException(e);
        }
    }

    private void injectCustomChunkStatus() {
        try {
            synchronized (IRegistry.CHUNK_STATUS) {
                Class chunkStatusA = Class.forName("net.minecraft.server.v1_14_R1.ChunkStatus$a");
                Field light = ReflectionHelper.Safe.getField(ChunkStatus.class, "LIGHT");
                Field biMap = ReflectionHelper.Safe.getField(RegistryMaterials.class, "c");
                Field fieldO = ReflectionHelper.Safe.getField(ChunkStatus.class, "o");
                Method register = ReflectionHelper.Safe.getMethod(ChunkStatus.class, "a", String.class, ChunkStatus.class, int.class, EnumSet.class, ChunkStatus.Type.class, chunkStatusA);

                Object aImplementation = Proxy.newProxyInstance(chunkStatusA.getClassLoader(), new Class[] {chunkStatusA},
                        (Object proxy, Method method, Object[] args) -> {
                            if (method.getName().equals("doWork")) {

                                System.out.println("doWork");

                                ChunkStatus status = (ChunkStatus) args[0];
                                WorldServer worldServer = (WorldServer) args[1];
                                LightEngineThreaded lightEngineThreaded = (LightEngineThreaded) args[4];
                                IChunkAccess iChunkAccess = new WrappedIChunkAccess(worldServer, (IChunkAccess) args[7]);

                                boolean flag = iChunkAccess.getChunkStatus().b(status) && iChunkAccess.r();

                                if (! iChunkAccess.getChunkStatus().b(status)) {
                                    ((ProtoChunk) args[7]).a(status);
                                }

                                return lightEngineThreaded.a(iChunkAccess, flag).thenAccept(Either::left);
                            }

                            return null;
                        }
                );

                IRegistry chunkStatus = IRegistry.CHUNK_STATUS;
                ((BiMap) ReflectionHelper.get(biMap, chunkStatus)).remove(new MinecraftKey("light"));
                ChunkStatus customLight = (ChunkStatus) ReflectionHelper.Safe.invokeStatic(register, "light", ChunkStatus.FEATURES, 1, ReflectionHelper.Safe.getStatic(fieldO), ChunkStatus.Type.PROTOCHUNK, aImplementation);

                ReflectionHelper.setStatic(light, customLight);
                LOGGER.info("Injected Custom Light ChunkStatus");
            }
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new NmsInitializationException(e);
        }
    }

    private void injectCustomIBlockAccess(World world) throws IllegalAccessException {
        WorldServer worldServer = getNmsWorld(world);
        LightEngineThreaded lightEngineThreaded = ReflectionHelper.Safe.get(LIGHT_ENGINE_FIELD_CHUNK_MAP, worldServer.getChunkProvider().playerChunkMap);

        ILightAccess custom = new ILightAccess() {
            private final IBlockAccess world = new WrappedIBlockAccess(worldServer, worldServer);

            @Override
            public IBlockAccess b(int chunkX, int chunkZ) {
                return new WrappedIBlockAccess(worldServer, worldServer.getChunkProvider().b(chunkX, chunkZ));
            }

            @Override
            public IBlockAccess getWorld() {
                return world;
            }
        };

        injectToEngine(ReflectionHelper.Safe.get(LIGHT_ENGINE_FIELD_CHUNK_MAP, worldServer.getChunkProvider().playerChunkMap), custom, EnumSkyBlock.BLOCK);
        injectToEngine(ReflectionHelper.Safe.get(LIGHT_ENGINE_FIELD_CHUNK_MAP, worldServer.getChunkProvider().playerChunkMap), custom, EnumSkyBlock.SKY);

        injectToEngine(worldServer.getChunkProvider().getLightEngine(), custom, EnumSkyBlock.BLOCK);
        injectToEngine(worldServer.getChunkProvider().getLightEngine(), custom, EnumSkyBlock.SKY);

        LOGGER.info(String.format("Injected custom IBlockAccess into world \"%s\"", world.getName()));
    }

    private void injectToEngine(LightEngineThreaded lightEngineThreaded, ILightAccess lightAccess, EnumSkyBlock enumSkyBlock) throws IllegalAccessException {
        LightEngineLayer engineLayer = (LightEngineLayer) Optional.ofNullable(lightEngineThreaded.a(enumSkyBlock)).orElse(null);

        if (engineLayer == null) {
            return;
        }

        ReflectionHelper.Safe.set(engineLayer, FIELD_LIGHT_ENGINE_LAYER_I_LIGHT_ACCESS, lightAccess);
    }

    private LightEngineThreaded getLightEngine(World world) {
        return getNmsWorld(world).getChunkProvider().getLightEngine();
    }

    private LightEngineLayer<?, ?> getLightEngineBlock(LightEngine lightEngine) {
        return (LightEngineLayer<?, ?>) lightEngine.a(EnumSkyBlock.BLOCK);
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private static BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static Location toLocation(BlockPosition blockPosition) {
        return new Location(null, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
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
                .getPersistor(INSTANCE.plugin, world.getWorld())
                .getEmittingLightLevel(toLocation(blockPosition)), world.getType(blockPosition).h());
    }

    @Override
    public void recalculateBlockLight(Location at) {
        LightEngineThreaded lightEngine = getLightEngine(at.getWorld());

        BlockPosition blockPosition = toBlockPosition(at);


    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent e) {
        try {
            injectCustomIBlockAccess(e.getWorld());
        } catch (IllegalAccessException ex) {
            throw new NmsInitializationException(ex);
        }
    }

    @Override
    public void updateBlockLight(Location at, int lightLevel) {

        WorldServer worldServer = getNmsWorld(at.getWorld());
        BlockPosition blockPosition = toBlockPosition(at);

//        try {
//            injectCustomLightEngine(worldServer);
//        } catch (IllegalAccessException e) {
//            throw new LightUpdateFailedException(e);
//        }

        PlayerChunkMap playerChunkMap = worldServer.getChunkProvider().playerChunkMap;

        LightEngineThreaded lightEngine = getLightEngine(at.getWorld());
        LightEngineLayer<?, ?> lightEngineLayer = getLightEngineBlock(lightEngine);

        lightEngineLayer.a(blockPosition, lightLevel); // Write light level to Block Light Engine

//        CompletableFuture<IChunkAccess> future = lightEngine.a(worldServer.getChunkAtWorldCoords(blockPosition), true);

        System.out.println(lightLevel);

        lightEngine.a(new WrappedIChunkAccess(worldServer, worldServer.getChunkAtWorldCoords(blockPosition)), true);

//        worldServer.getChunkAtWorldCoords(blockPosition).b(false);
//        playerChunkMap.a(playerChunkMap.visibleChunks.get(ChunkCoordIntPair.pair(blockPosition.getX() >> 4, blockPosition.getZ() >> 4)), ChunkStatus.LIGHT);
        //.thenRun(() -> lightEngine.a(blockPosition));

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
