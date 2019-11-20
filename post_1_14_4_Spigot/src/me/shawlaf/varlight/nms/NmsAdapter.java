package me.shawlaf.varlight.nms;

import com.google.common.collect.BiMap;
import com.mojang.datafixers.util.Either;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.IntPosition;
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
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.IntSupplier;

@SuppressWarnings("deprecation")
@ForMinecraft(version = "Spigot 1.14.4")
public class NmsAdapter implements INmsAdapter, Listener {

    private static final String TAG_VARLIGHT_INJECTED = "varlight:injected";

    private static final BlockFace[] CHECK_FACES = new BlockFace[]{
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH
    };

    private final Field fieldLightBlocking, fieldLightEngineChunkMap, fieldLightEngineLayerILightAccess;

    private final VarLightPlugin plugin;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

        if (plugin.isPaper()) {
            throw new VarLightInitializationException("You are using the Spigot implementation on a Paper Server!");
        }

        try {
            fieldLightBlocking = ReflectionHelper.Safe.getField(net.minecraft.server.v1_14_R1.Block.class, "v");
            fieldLightEngineChunkMap = ReflectionHelper.Safe.getField(PlayerChunkMap.class, "lightEngine");
            fieldLightEngineLayerILightAccess = ReflectionHelper.Safe.getField(LightEngineLayer.class, "a");
        } catch (NoSuchFieldException e) {
            throw new VarLightInitializationException(e);
        }
    }

    @Override
    public boolean isLightApiAllowed() {
        return false;
    }

    @Override
    public void onLoad() {
        injectCustomChunkStatus();
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onWorldEnable(@NotNull World world) {
        try {
            injectCustomIBlockAccess(world);
        } catch (IllegalAccessException e) {
            throw new VarLightInitializationException(e);
        }
    }

    @Override
    public boolean isBlockTransparent(@NotNull Block block) {
        try {
            return !(boolean) ReflectionHelper.Safe.get(fieldLightBlocking, getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock());
        } catch (IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    @Override
    public void updateBlockLight(@NotNull final Location at, final int lightLevel) {
        final WorldServer worldServer = getNmsWorld(at.getWorld());
        final BlockPosition position = toBlockPosition(at);
        final LightEngineThreaded lightEngineThreaded = worldServer.getChunkProvider().getLightEngine();
        final LightEngineBlock lightEngineBlock = (LightEngineBlock) lightEngineThreaded.a(EnumSkyBlock.BLOCK);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            net.minecraft.server.v1_14_R1.Chunk chunk = worldServer.getChunkProvider().getChunkAt(position.getX() >> 4, position.getZ() >> 4, false);

            if (chunk == null || !chunk.loaded) {
                return;
            }

            updateBlockAndChunk(worldServer, at, chunk);

            if (lightLevel > 0) {
                lightEngineBlock.a(position, lightLevel);
            }

            if (!lightEngineThreaded.a()) {
                return;
            }

            updateBlockAndChunk(worldServer, at, chunk);
        });
    }

    @Override
    public int getEmittingLightLevel(@NotNull Block block) {
        IBlockData blockData = ((CraftBlock) block).getNMS();

        return blockData.getBlock().a(blockData);
    }

    @Override
    @Deprecated
    public void sendChunkUpdates(@NotNull Chunk chunk, int mask) {
        throw new UnsupportedOperationException("Not used in this version");
    }

    @Override
    public boolean isIllegalBlock(@NotNull Block block) {
        if (!block.getType().isBlock()) {
            return true;
        }

        if (getEmittingLightLevel(block) > 0) {
            return true;
        }

        BlockData blockData = block.getType().createBlockData();

        if (blockData instanceof Powerable || blockData instanceof AnaloguePowerable || blockData instanceof Openable || blockData instanceof Piston) {
            return true;
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return true;
        }

        if (block.getType() == Material.BLUE_ICE) {
            return false; // Packed ice is solid and occluding but blue ice isn't?
        }

        return !block.getType().isSolid() || !block.getType().isOccluding();
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

    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

    public int getCustomLuminance(WorldServer worldServer, BlockPosition pos, IntSupplier def) {
        WorldLightSourceManager manager = WorldLightSourceManager.getManager(plugin, worldServer.getWorld());

        if (manager == null) {
            return def.getAsInt();
        }

        return manager.getCustomLuminance(new IntPosition(pos.getX(), pos.getY(), pos.getZ()), def);
    }

    @Override
    public Block getTargetBlockExact(Player player, int maxDistance) {
        return player.getTargetBlockExact(maxDistance, FluidCollisionMode.NEVER);
    }

    // region Util Methods

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private void updateBlockAndChunk(WorldServer worldServer, Location location, net.minecraft.server.v1_14_R1.Chunk chunk) {
        final LightEngineThreaded lightEngine = worldServer.getChunkProvider().getLightEngine();

        lightEngine.a(toBlockPosition(location));

        CompletableFuture<IChunkAccess> future = lightEngine.a(new WrappedIChunkAccess(this, worldServer, chunk), true);

        Runnable updateChunkTask = () -> {
            for (Chunk c : collectChunksToUpdate(location)) {
                queueChunkLightUpdate(c, location.getBlockY() >> 4);
            }
        };

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

    public void queueChunkLightUpdate(Chunk chunk, int sectionY) {
        WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
        PlayerChunkMap playerChunkMap = nmsWorld.getChunkProvider().playerChunkMap;
        PlayerChunk playerChunk = playerChunkMap.visibleChunks.get(ChunkCoordIntPair.pair(chunk.getX(), chunk.getZ()));
        ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());

        int mask = getChunkBitMask(sectionY);

        PacketPlayOutLightUpdate packetPlayOutLightUpdate = new PacketPlayOutLightUpdate(chunkCoordIntPair, nmsWorld.getChunkProvider().getLightEngine(), 0, mask << 1); // Mask has to be shifted, because LSB is section Y -1

        playerChunk.players.a(chunkCoordIntPair, false).forEach(e -> e.playerConnection.sendPacket(packetPlayOutLightUpdate));
    }

    // endregion

    // region Injection Methods

    // region Chunk Status
    private void injectCustomChunkStatus() {
        try {
            Class chunkStatusB = Class.forName("net.minecraft.server.v1_14_R1.ChunkStatus$b");
            Class chunkStatusC = Class.forName("net.minecraft.server.v1_14_R1.ChunkStatus$c");

            Field light = ReflectionHelper.Safe.getField(ChunkStatus.class, "LIGHT");
            Field biMap = ReflectionHelper.Safe.getField(RegistryMaterials.class, "c");
            Field fieldO = ReflectionHelper.Safe.getField(ChunkStatus.class, "o");
            Method register = ReflectionHelper.Safe.getMethod(ChunkStatus.class, "a", String.class, ChunkStatus.class, int.class, EnumSet.class, ChunkStatus.Type.class, chunkStatusB, chunkStatusC);

            final IRegistry chunkStatus = IRegistry.CHUNK_STATUS;

            Object bImplementation = Proxy.newProxyInstance(chunkStatusB.getClassLoader(), new Class[]{chunkStatusB},
                    (Object proxy, Method method, Object[] args) -> {
                        if (method.getName().equals("doWork")) {
                            ChunkStatus status = (ChunkStatus) args[0];
                            WorldServer worldServer = (WorldServer) args[1];
                            LightEngineThreaded lightEngineThreaded = (LightEngineThreaded) args[4];
                            IChunkAccess iChunkAccess = (IChunkAccess) args[7];

                            return doWork(status, worldServer, iChunkAccess, lightEngineThreaded);
                        }

                        return null;
                    }
            );

            Object cImplementation = Proxy.newProxyInstance(chunkStatusC.getClassLoader(), new Class[]{chunkStatusC},
                    (Object proxy, Method method, Object[] args) -> {
                        if (method.getName().equals("doWork")) {
                            ChunkStatus status = (ChunkStatus) args[0];
                            WorldServer worldServer = (WorldServer) args[1];
                            LightEngineThreaded lightEngineThreaded = (LightEngineThreaded) args[3];
                            IChunkAccess iChunkAccess = (IChunkAccess) args[5];

                            return doWork(status, worldServer, iChunkAccess, lightEngineThreaded);
                        }
                        return null;
                    }
            );

            ((BiMap) ReflectionHelper.get(biMap, chunkStatus)).remove(new MinecraftKey("light"));
            ChunkStatus customLight = (ChunkStatus) ReflectionHelper.Safe.invokeStatic(register, "light", ChunkStatus.FEATURES, 1, ReflectionHelper.Safe.getStatic(fieldO), ChunkStatus.Type.PROTOCHUNK, bImplementation, cImplementation);

            ReflectionHelper.setStatic(light, customLight);
            plugin.getLogger().info("Injected Custom Light ChunkStatus");

        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new VarLightInitializationException(e);
        }
    }

    private CompletableFuture<Void> doWork(ChunkStatus status, WorldServer worldServer, IChunkAccess iChunkAccess, LightEngineThreaded lightEngineThreaded) {
        boolean useWrapped = WorldLightSourceManager.hasManager(plugin, worldServer.getWorld());

        boolean flag = iChunkAccess.getChunkStatus().b(status) && iChunkAccess.r();
        IChunkAccess wrapped = useWrapped ? new WrappedIChunkAccess(this, worldServer, iChunkAccess) : iChunkAccess;

        if (!wrapped.getChunkStatus().b(status)) {
            ((ProtoChunk) iChunkAccess).a(status);
        }

        return lightEngineThreaded.a(wrapped, flag).thenAccept(Either::left);
    }

    // endregion

    // region Block Access

    private void injectCustomIBlockAccess(World world) throws IllegalAccessException {
        if (!WorldLightSourceManager.hasManager(plugin, world)) {
            return;
        }

        if (world.hasMetadata(TAG_VARLIGHT_INJECTED) && world.getMetadata(TAG_VARLIGHT_INJECTED).get(0).asBoolean()) {
            return;
        }

        WorldServer worldServer = getNmsWorld(world);

        ILightAccess customLightAccess = new ILightAccess() {
            private final IBlockAccess world = new WrappedIBlockAccess(NmsAdapter.this, worldServer, worldServer);

            @Nullable
            @Override
            public IBlockAccess c(int chunkX, int chunkZ) {
                IBlockAccess toWrap = worldServer.getChunkProvider().c(chunkX, chunkZ);

                if (toWrap == null) {
                    return null;
                }

                return new WrappedIBlockAccess(NmsAdapter.this, worldServer, toWrap);
            }

            @Override
            public IBlockAccess getWorld() {
                return world;
            }
        };

        // TODO inject
        injectCustomLightAccess(ReflectionHelper.Safe.get(fieldLightEngineChunkMap, worldServer.getChunkProvider().playerChunkMap), customLightAccess);
        injectCustomLightAccess(worldServer.getChunkProvider().getLightEngine(), customLightAccess);

        world.setMetadata(TAG_VARLIGHT_INJECTED, new FixedMetadataValue(plugin, true));

        plugin.getLogger().info(String.format("Injected custom IBlockAccess into world \"%s\"", world.getName()));
    }

    private void injectCustomLightAccess(LightEngineThreaded lightEngine, ILightAccess toInject) throws IllegalAccessException {
        LightEngineLayerEventListener engineLayer = lightEngine.a(EnumSkyBlock.BLOCK);

        if (engineLayer == LightEngineLayer.Void.INSTANCE) {
            return;
        }

        ReflectionHelper.Safe.set(engineLayer, fieldLightEngineLayerILightAccess, toInject);

    }

    // endregion

    // endregion

    // region Events

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        handleBlockUpdate(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        handleBlockUpdate(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent e) {
        handleBlockUpdate(e);
    }

    private void handleBlockUpdate(BlockEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block theBlock = e.getBlock();
            WorldServer worldServer = getNmsWorld(theBlock.getWorld());

            for (BlockFace blockFace : CHECK_FACES) {
                BlockPosition relative = toBlockPosition(theBlock.getLocation().add(blockFace.getDirection()));

                if (getCustomLuminance(worldServer, relative, () -> worldServer.getType(relative).h()) > 0 && worldServer.getType(relative).h() == 0) {
                    int sectionY = theBlock.getY() / 16;
                    collectChunksToUpdate(theBlock.getLocation()).forEach(c -> queueChunkLightUpdate(c, sectionY));
                    return;
                }
            }
        }, 1L);
    }

    // endregion


}
