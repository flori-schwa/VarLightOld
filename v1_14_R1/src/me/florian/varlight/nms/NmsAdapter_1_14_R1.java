package me.florian.varlight.nms;


import com.google.common.collect.BiMap;
import com.mojang.datafixers.util.Either;
import me.florian.varlight.LightUpdaterBuiltIn;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.material.Openable;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class NmsAdapter_1_14_R1 implements NmsAdapter, Listener {

    private static final BlockFace[] CHECK_FACES = new BlockFace[] {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH
    };

    private static NmsAdapter_1_14_R1 INSTANCE = null;

    private final Field fieldLightBlocking, fieldLightEngineChunkMap, fieldLightEngineLayerILightAccess;

    public NmsAdapter_1_14_R1() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Created second instance of Singleton Object " + getClass().getName());
        }

        INSTANCE = this;

        try {
            fieldLightBlocking = ReflectionHelper.Safe.getField(net.minecraft.server.v1_14_R1.Block.class, "v");
            fieldLightEngineChunkMap = ReflectionHelper.Safe.getField(PlayerChunkMap.class, "lightEngine");
            fieldLightEngineLayerILightAccess = ReflectionHelper.Safe.getField(LightEngineLayer.class, "a");

        } catch (NoSuchFieldException e) {
            throw new NmsInitializationException(e);
        }
    }

    private VarLightPlugin plugin;

    @Override
    public void onLoad(Plugin plugin, boolean use) {
        this.plugin = (VarLightPlugin) plugin;

        if (! use) {
            return;
        }

        injectCustomChunkStatus();
    }

    @Override
    public void onEnable(Plugin plugin, boolean use) {
        if (! use) {
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        try {
            for (World world : Bukkit.getWorlds()) {
                injectCustomIBlockAccess(world);
            }
        } catch (IllegalAccessException e) {
            throw new NmsInitializationException(e);
        }

        this.plugin.setLightUpdater(new LightUpdaterBuiltIn(this.plugin) {
            @Override
            public void setLight(Location location, int lightLevel) {
                WorldServer worldServer = NmsAdapter_1_14_R1.this.getNmsWorld(location.getWorld());
                BlockPosition blockPosition = NmsAdapter_1_14_R1.toBlockPosition(location);

                LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

                LightEngineBlock lightEngineBlock = (LightEngineBlock) lightEngine.a(EnumSkyBlock.BLOCK);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    update(worldServer, location, blockPosition, lightEngine);

                    if (lightLevel > 0) {
                        lightEngineBlock.a(blockPosition, lightLevel);
                    }

                    if (! lightEngine.a()) {
                        return;
                    }

                    update(worldServer, location, blockPosition, lightEngine);
                });
            }

            private void update(WorldServer worldServer, Location location, BlockPosition blockPosition, LightEngineThreaded lightEngineThreaded) {
                lightEngineThreaded.a(blockPosition);
                Future future = lightEngineThreaded.a(new WrappedIChunkAccess(worldServer, worldServer.getChunkAtWorldCoords(blockPosition)), true);

                Runnable updateChunkTask = () -> ((VarLightPlugin) plugin).getLightUpdater()
                        .getChunksToUpdate(location)
                        .forEach(c -> queueChunkLightUpdate(c, location.getBlockY() / 16));

                if (worldServer.getMinecraftServer().isMainThread()) {
                    worldServer.getMinecraftServer().awaitTasks(future::isDone);
                    updateChunkTask.run();
                } else {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, updateChunkTask);
                }
            }
        });
    }

    private void injectCustomChunkStatus() {
        try {
            Class chunkStatusA = Class.forName("net.minecraft.server.v1_14_R1.ChunkStatus$a");
            Field light = ReflectionHelper.Safe.getField(ChunkStatus.class, "LIGHT");
            Field biMap = ReflectionHelper.Safe.getField(RegistryMaterials.class, "c");
            Field fieldO = ReflectionHelper.Safe.getField(ChunkStatus.class, "o");
            Method register = ReflectionHelper.Safe.getMethod(ChunkStatus.class, "a", String.class, ChunkStatus.class, int.class, EnumSet.class, ChunkStatus.Type.class, chunkStatusA);

            Object aImplementation = Proxy.newProxyInstance(chunkStatusA.getClassLoader(), new Class[] {chunkStatusA},
                    (Object proxy, Method method, Object[] args) -> {
                        if (method.getName().equals("doWork")) {

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
            plugin.getLogger().info("Injected Custom Light ChunkStatus");
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new NmsInitializationException(e);
        }
    }

    private void injectCustomIBlockAccess(World world) throws IllegalAccessException {
        WorldServer worldServer = getNmsWorld(world);

        ILightAccess custom = new ILightAccess() {
            private final IBlockAccess world = new WrappedIBlockAccess(worldServer, worldServer);

            @Override
            public IBlockAccess b(int chunkX, int chunkZ) {

                IBlockAccess toWrap = worldServer.getChunkProvider().b(chunkX, chunkZ);

                if (toWrap == null) {
                    return null;
                }

                return new WrappedIBlockAccess(worldServer, toWrap);
            }

            @Override
            public IBlockAccess getWorld() {
                return world;
            }
        };

        injectToEngine(ReflectionHelper.Safe.get(fieldLightEngineChunkMap, worldServer.getChunkProvider().playerChunkMap), custom);
        injectToEngine(worldServer.getChunkProvider().getLightEngine(), custom);

        plugin.getLogger().info(String.format("Injected custom IBlockAccess into world \"%s\"", world.getName()));
    }

    private void injectToEngine(LightEngineThreaded lightEngineThreaded, ILightAccess lightAccess) throws IllegalAccessException {
        LightEngineLayerEventListener engineLayer = lightEngineThreaded.a(EnumSkyBlock.BLOCK);

        if (engineLayer == LightEngineLayerEventListener.Void.INSTANCE) {
            return;
        }

        ReflectionHelper.Safe.set(engineLayer, fieldLightEngineLayerILightAccess, lightAccess);
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

    protected static int getCustomLuminance(WorldServer world, BlockPosition blockPosition, Supplier<Integer> def) {
        return LightSourcePersistor.getPersistor(INSTANCE.plugin, world.getWorld()).getEmittingLightLevel(toLocation(blockPosition), def.get());
    }

    @Override
    public boolean isBlockTransparent(Block block) {
        try {
            return ! (boolean) ReflectionHelper.Safe.get(fieldLightBlocking, getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock());
        } catch (IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent e) {
        try {
            injectCustomIBlockAccess(e.getWorld());
        } catch (IllegalAccessException ex) {
            throw new NmsInitializationException(ex);
        }
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        handleBlockUpdate(e);
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        handleBlockUpdate(e);
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent e) {
        handleBlockUpdate(e);
    }

    private void handleBlockUpdate(BlockEvent e) {
        Block theBlock = e.getBlock();
        WorldServer worldServer = getNmsWorld(theBlock.getWorld());

        for (BlockFace blockFace : CHECK_FACES) {
            BlockPosition relative = toBlockPosition(theBlock.getLocation().add(blockFace.getDirection()));

            if (getCustomLuminance(worldServer, relative, () -> worldServer.getType(relative).h()) > 0 && worldServer.getType(relative).h() == 0) {
                int sectionY = theBlock.getY() / 16;
                plugin.getLightUpdater().getChunksToUpdate(theBlock.getLocation()).forEach(c -> queueChunkLightUpdate(c, sectionY));
                return;
            }
        }
    }

    @Override
    @Deprecated
    public void recalculateBlockLight(Location at) {
        throw new UnsupportedOperationException("Not used for v1_14_R1!");
    }

    @Override
    @Deprecated
    public void updateBlockLight(Location at, int lightLevel) {
        throw new UnsupportedOperationException("Not used for v1_14_R1!");
    }

    @Override
    @Deprecated
    public void sendChunkUpdates(Chunk chunk) {
        throw new UnsupportedOperationException("Not used for v1_14_R1!");
    }

    @Override
    @Deprecated
    public void sendChunkUpdates(Chunk chunk, int mask) {
        throw new UnsupportedOperationException("Not used for v1_14_R1!");
    }

    @Override
    public int getEmittingLightLevel(Block block) {
        IBlockData blockData = ((CraftWorld) block.getWorld()).getHandle().getChunkAt(block.getChunk().getX(), block.getChunk().getZ()).getType(toBlockPosition(block.getLocation()));

        return blockData.h();
    }

    public void queueChunkLightUpdate(Chunk chunk, int sectionY) {
        WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
        PlayerChunkMap playerChunkMap = nmsWorld.getChunkProvider().playerChunkMap;
        PlayerChunk playerChunk = playerChunkMap.visibleChunks.get(ChunkCoordIntPair.pair(chunk.getX(), chunk.getZ()));
        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());

        int mask = plugin.getLightUpdater().getChunkBitMask(sectionY);

        PacketPlayOutLightUpdate packetPlayOutLightUpdate = new PacketPlayOutLightUpdate(chunkCoordIntPair, nmsWorld.getChunkProvider().getLightEngine(), 0, mask << 1); // Mask has to be shifted, because LSB is section Y -1

        playerChunk.players.a(chunkCoordIntPair, false).forEach(e -> e.playerConnection.sendPacket(packetPlayOutLightUpdate));
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

    @Override
    public void setCooldown(Player player, Material material, int ticks) {
        player.setCooldown(material, ticks);
    }

    @Override
    public boolean hasCooldown(Player player, Material material) {
        return player.hasCooldown(material);
    }
}
