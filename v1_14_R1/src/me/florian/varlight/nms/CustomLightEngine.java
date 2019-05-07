package me.florian.varlight.nms;

import net.minecraft.server.v1_14_R1.*;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

public class CustomLightEngine extends LightEngineThreaded {

    private static final Object PRE_UPDATE, POST_UPDATE;
    private static final Method METHOD_INIT_UPDATE;
    private static final Field FIELD_LIGHT_ENGINE_BLOCK, FIELD_LIGHT_ENGINE_SKY, FIELD_THREADED_MAILBOX, FIELD_MAILBOX, FIELD_LIGHT_ENGINE_LAYER_I_LIGHT_ACCESS;

    static {
        try {
            Class updateClass = Class.forName("net.minecraft.server.v1_14_R1.LightEngineThreaded$Update");
            Method valuesMethod = ReflectionHelper.Safe.getMethod(updateClass, "values");

            Object[] values = (Object[]) ReflectionHelper.Safe.invokeStatic(valuesMethod);

            PRE_UPDATE = values[0];
            POST_UPDATE = values[1];

            METHOD_INIT_UPDATE = ReflectionHelper.Safe.getMethod(LightEngineThreaded.class, "a", int.class, int.class, updateClass, Runnable.class);

            FIELD_LIGHT_ENGINE_BLOCK = ReflectionHelper.Safe.getField(LightEngine.class, "a");
            FIELD_LIGHT_ENGINE_SKY = ReflectionHelper.Safe.getField(LightEngine.class, "b");

            FIELD_THREADED_MAILBOX = ReflectionHelper.Safe.getField(LightEngineThreaded.class, "b");
            FIELD_MAILBOX = ReflectionHelper.Safe.getField(LightEngineThreaded.class, "e");

            FIELD_LIGHT_ENGINE_LAYER_I_LIGHT_ACCESS = ReflectionHelper.Safe.getField(LightEngineLayer.class, "a");
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new NmsInitializationException(e);
        }
    }

    private WorldServer worldServer;
    private LightEngineThreaded wrapped;

    public CustomLightEngine(LightEngineThreaded wrapped, WorldServer worldServer) throws IllegalAccessException {
        super(
                worldServer.getChunkProvider(),
                worldServer.getChunkProvider().playerChunkMap,
                worldServer.getWorldProvider().g(),
                ReflectionHelper.Safe.get(FIELD_THREADED_MAILBOX, wrapped),
                ReflectionHelper.Safe.get(FIELD_MAILBOX, wrapped));


        ILightAccess custom = new ILightAccess() {
            private final IBlockAccess cachedForWorld = new WrappedIBlockAccess(worldServer, worldServer);
            private final Long2ObjectLinkedOpenHashMap<WrappedIBlockAccess> chunks = new Long2ObjectLinkedOpenHashMap<>();


            @Override
            public IBlockAccess b(int chunkX, int chunkZ) {
//                        long encoded = ChunkCoordIntPair.pair(chunkX, chunkZ);
//
//                        if (chunks.containsKey(encoded) && chunks.get(encoded).) {
//                            return chunks.get(encoded);
//                        }
//
                IBlockAccess iBlockAccess = worldServer.getChunkProvider().b(chunkX, chunkZ);
//
//                        if (iBlockAccess == null) {
//                            return null;
//                        }
//
//                        WrappedIBlockAccess wrapped = ;
//                        chunks.put(encoded, wrapped);
                return new WrappedIBlockAccess(worldServer, iBlockAccess);
            }

            @Override
            public IBlockAccess getWorld() {
                return cachedForWorld;
            }
        };

        this.worldServer = worldServer;
        this.wrapped = wrapped;

        ReflectionHelper.Safe.set(this, FIELD_LIGHT_ENGINE_BLOCK, wrapped.a(EnumSkyBlock.BLOCK));
        ReflectionHelper.Safe.set(this, FIELD_LIGHT_ENGINE_SKY, wrapped.a(EnumSkyBlock.SKY));

        injectToBlockEngine(custom, EnumSkyBlock.BLOCK);
        injectToBlockEngine(custom, EnumSkyBlock.SKY);
    }

    private void injectToBlockEngine(ILightAccess lightAccess, EnumSkyBlock enumSkyBlock) throws IllegalAccessException {
        LightEngineLayer engineLayer = (LightEngineLayer) Optional.ofNullable(a(enumSkyBlock)).orElse(null);

        if (engineLayer == null) {
            return;
        }

        ReflectionHelper.Safe.set(engineLayer, FIELD_LIGHT_ENGINE_LAYER_I_LIGHT_ACCESS, lightAccess);
    }

    private void initUpdate(int x, int z, Object update, Runnable runnable) {
        try {
            ReflectionHelper.Safe.invoke(this, METHOD_INIT_UPDATE, x, z, update, runnable);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    private void updateSection(SectionPosition sectionPosition, boolean flag) {
        Optional.ofNullable(a(EnumSkyBlock.BLOCK)).ifPresent(l -> l.a(sectionPosition, flag));
        Optional.ofNullable(a(EnumSkyBlock.SKY)).ifPresent(l -> l.a(sectionPosition, flag));
    }

    private void updateChunk(ChunkCoordIntPair chunkCoordIntPair, boolean flag) {
        Optional.ofNullable(a(EnumSkyBlock.BLOCK)).map(l -> (LightEngineLayer) l).ifPresent(l -> l.a(chunkCoordIntPair, flag));
        Optional.ofNullable(a(EnumSkyBlock.SKY)).map(l -> (LightEngineLayer) l).ifPresent(l -> l.a(chunkCoordIntPair, flag));
    }

    @Override
    public CompletableFuture<IChunkAccess> a(IChunkAccess iChunkAccess, boolean flag) {
        ChunkCoordIntPair chunkCoordIntPair = iChunkAccess.getPos();

        System.out.println("lightChunk " + chunkCoordIntPair + " " + flag);
//        Thread.dumpStack();

        initUpdate(chunkCoordIntPair.x, chunkCoordIntPair.z, PRE_UPDATE, SystemUtils.a(() -> {

            System.out.println("Running");

            ChunkSection[] sections = iChunkAccess.getSections();

            for (int sectionY = 0; sectionY < 16; sectionY++) {
                ChunkSection section = sections[sectionY];

                if (! ChunkSection.a(section)) {
                    updateSection(SectionPosition.a(chunkCoordIntPair, sectionY), false);
                }
            }

            updateChunk(chunkCoordIntPair, true);

            final LightEngineBlock lightEngineBlock = (LightEngineBlock) a(EnumSkyBlock.BLOCK);

            if (! flag) {
                StreamSupport.stream(BlockPosition.b(iChunkAccess.getPos().d(), 0, iChunkAccess.getPos().e(), iChunkAccess.getPos().f(), 255, iChunkAccess.getPos().g()).spliterator(), false).forEach(pos -> {
                    int brightness = NmsAdapter_1_14_R1.getBrightness(worldServer, pos);

                    Logger.getLogger(getClass().getName()).info(String.valueOf(brightness));

                    if (brightness == 0) {
                        return;
                    }

                    lightEngineBlock.a(pos, brightness);
                });
            }


        }, () -> "lightChunk " + chunkCoordIntPair + " " + flag));

        return CompletableFuture.supplyAsync(
                () -> iChunkAccess,
                command -> initUpdate(chunkCoordIntPair.x, chunkCoordIntPair.z, POST_UPDATE, command)
        );
    }

    @Override
    public void a(BlockPosition var0) {
        wrapped.a(var0);
    }

    @Override
    public void a(ChunkCoordIntPair var0, boolean var1) {
        wrapped.a(var0, var1);
    }

    @Override
    public void a(EnumSkyBlock var0, SectionPosition var1, NibbleArray var2) {
        wrapped.a(var0, var1, var2);
    }

    @Override
    public void queueUpdate() {
        wrapped.queueUpdate();
    }
}
