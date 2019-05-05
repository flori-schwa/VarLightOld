package me.florian.varlight.nms;

import net.minecraft.server.v1_14_R1.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

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

    private LightEngineThreaded base;

    public CustomLightEngine(LightEngineThreaded base, WorldServer worldServer) throws IllegalAccessException {
        super(
                worldServer.getChunkProvider(),
                worldServer.getChunkProvider().playerChunkMap,
                worldServer.getWorldProvider().g(),
                ReflectionHelper.Safe.get(FIELD_THREADED_MAILBOX, base),
                ReflectionHelper.Safe.get(FIELD_MAILBOX, base));

        this.base = base;

//        LightEngineBlock blockLayer = ReflectionHelper.Safe.get(FIELD_LIGHT_ENGINE_BLOCK, base);

        ReflectionHelper.Safe.set(this, FIELD_LIGHT_ENGINE_BLOCK, ReflectionHelper.Safe.get(FIELD_LIGHT_ENGINE_BLOCK, base));
        ReflectionHelper.Safe.set(this, FIELD_LIGHT_ENGINE_SKY, ReflectionHelper.Safe.get(FIELD_LIGHT_ENGINE_SKY, base));
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
                iChunkAccess.m().forEach(blockPos -> {
                            int emitting = iChunkAccess.h(blockPos);
                            int brightness = lightEngineBlock.b(blockPos);

                            System.out.printf("e: %d, b: %d%n", emitting, brightness);

                            updateLight(blockPos, Math.max(emitting, brightness));
                        }
                );
            }


        }, () -> "lightChunk " + chunkCoordIntPair + " " + flag));

        return CompletableFuture.supplyAsync(
                () -> iChunkAccess,
                command -> initUpdate(chunkCoordIntPair.x, chunkCoordIntPair.z, POST_UPDATE, command)
        );
    }

    @Override
    public void a(BlockPosition var0) {
        base.a(var0);
    }

    @Override
    public void a(ChunkCoordIntPair var0, boolean var1) {
        base.a(var0, var1);
    }

    @Override
    public void a(EnumSkyBlock var0, SectionPosition var1, NibbleArray var2) {
        base.a(var0, var1, var2);
    }

    @Override
    public void queueUpdate() {
        base.queueUpdate();
    }
}
