package me.florian.varlight.nms;

import net.minecraft.server.v1_14_R1.*;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

public class CustomLightEngine extends LightEngineThreaded {

    private static final Object PRE_UPDATE, POST_UPDATE;
    private static final Method METHOD_INIT_UPDATE, METHOD_UPDATE_SECTION, METHOD_UPDATE_CHUNK, METHOD_UPDATE_LIGHT;
    private static final Field FIELD_LIGHT_ENGINE_BLOCK, FIELD_LIGHT_ENGINE_SKY, FIELD_THREADED_MAILBOX, FIELD_MAILBOX;

    static {
        try {
            Class updateClass = Class.forName("net.minecraft.server.v1_14_R1.LightEngineThreaded$Update");
            Method valuesMethod = ReflectionHelper.Safe.getMethod(updateClass, "values");

            Object[] values = (Object[]) ReflectionHelper.Safe.invokeStatic(valuesMethod);

            PRE_UPDATE = values[0];
            POST_UPDATE = values[1];

            METHOD_INIT_UPDATE = ReflectionHelper.Safe.getMethod(LightEngineThreaded.class, "a", int.class, int.class, updateClass, Runnable.class);
            METHOD_UPDATE_SECTION = ReflectionHelper.Safe.getMethod(LightEngine.class, "a", SectionPosition.class, boolean.class); // TODO This doesn't work, you cannot access the superclass' implementation using reflection
            METHOD_UPDATE_CHUNK = ReflectionHelper.Safe.getMethod(LightEngine.class, "a", ChunkCoordIntPair.class, boolean.class);
            METHOD_UPDATE_LIGHT = ReflectionHelper.Safe.getMethod(LightEngine.class, "a", BlockPosition.class, int.class);

            FIELD_LIGHT_ENGINE_BLOCK = ReflectionHelper.getField(LightEngine.class, "a");
            FIELD_LIGHT_ENGINE_SKY = ReflectionHelper.getField(LightEngine.class, "b");

            FIELD_THREADED_MAILBOX = ReflectionHelper.getField(LightEngineThreaded.class, "b");
            FIELD_MAILBOX = ReflectionHelper.getField(LightEngineThreaded.class, "e");
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new NmsInitializationException(e);
        }
    }

    private LightEngineThreaded wrapped;

    public CustomLightEngine(LightEngineThreaded wrapped, WorldServer worldServer) throws IllegalAccessException {
        super(
                new ILightAccess() {
                    private final IBlockAccess cachedForWorld = new WrappedIBlockAccess(worldServer);
                    private final Long2ObjectLinkedOpenHashMap<WrappedIBlockAccess> chunks = new Long2ObjectLinkedOpenHashMap<>();

                    @Nullable
                    @Override
                    public IBlockAccess b(int chunkX, int chunkZ) {
                        long encoded = ChunkCoordIntPair.pair(chunkX, chunkZ);

                        if (chunks.containsKey(encoded)) {
                            return chunks.get(encoded);
                        }

                        IBlockAccess iBlockAccess = worldServer.getChunkProvider().b(chunkX, chunkZ);

                        if (iBlockAccess == null) {
                            return null;
                        }

                        WrappedIBlockAccess wrapped = new WrappedIBlockAccess(iBlockAccess);
                        chunks.put(encoded, wrapped);
                        return wrapped;
                    }

                    @Override
                    public IBlockAccess getWorld() {
                        return cachedForWorld;
                    }
                },
                worldServer.getChunkProvider().playerChunkMap,
                worldServer.getWorldProvider().g(),
                ReflectionHelper.Safe.get(FIELD_THREADED_MAILBOX, wrapped),
                ReflectionHelper.Safe.get(FIELD_MAILBOX, wrapped));

        this.wrapped = wrapped;

        ReflectionHelper.Safe.set(this, FIELD_LIGHT_ENGINE_BLOCK, ReflectionHelper.Safe.get(FIELD_LIGHT_ENGINE_BLOCK, wrapped));
        ReflectionHelper.Safe.set(this, FIELD_LIGHT_ENGINE_SKY, ReflectionHelper.Safe.get(FIELD_LIGHT_ENGINE_SKY, wrapped));
    }

    private void initUpdate(int x, int z, Object update, Runnable runnable) {
        try {
            ReflectionHelper.Safe.invoke(this, METHOD_INIT_UPDATE, x, z, update, runnable);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    private void updateSection(SectionPosition sectionPosition, boolean flag) {
        try {
            ReflectionHelper.Safe.invoke(this, METHOD_UPDATE_SECTION, sectionPosition, flag);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    private void updateChunk(ChunkCoordIntPair chunkCoordIntPair, boolean flag) {
        try {
            ReflectionHelper.Safe.invoke(this, METHOD_UPDATE_CHUNK, chunkCoordIntPair, flag);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    private void updateLight(BlockPosition blockPosition, int lightLevel) {
        try {
            ReflectionHelper.Safe.invoke(this, METHOD_UPDATE_LIGHT, blockPosition, lightLevel);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
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
                    int emitting = iChunkAccess.h(pos);
                    int brightness = lightEngineBlock.b(pos);

                    if (emitting == 0 || brightness == 0) {
                        return;
                    }

                    lightEngineBlock.a(pos, Math.max(emitting, brightness));
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
