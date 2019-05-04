package me.florian.varlight.nms;

import net.minecraft.server.v1_14_R1.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomLightEngineBlock extends LightEngineLayer<LightEngineStorageBlock.a, LightEngineStorageBlock> {

    // TODO injecting this causes the server to freeze while shutting down

    private static final Field FIELD_LIGHT_ENGINE_STORAGE;
    private static final Method METHOD_B, METHOD_A, METHOD_OTHER_A, METHOD_C;

    static {
        try {
            FIELD_LIGHT_ENGINE_STORAGE = ReflectionHelper.Safe.getField(LightEngineLayer.class, "c");

            METHOD_B = ReflectionHelper.Safe.getMethod(LightEngineBlock.class, "b", long.class, long.class, int.class);
            METHOD_A = ReflectionHelper.Safe.getMethod(LightEngineBlock.class, "a", long.class, int.class, boolean.class);
            METHOD_OTHER_A = ReflectionHelper.Safe.getMethod(LightEngineBlock.class, "a", long.class, long.class, int.class);

            METHOD_C = ReflectionHelper.Safe.getMethod(LightEngineStorage.class, "c");
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new NmsInitializationException(e);
        }
    }

    private LightEngineBlock base;

    public CustomLightEngineBlock(LightEngineBlock base, WorldServer worldServer) throws IllegalAccessException {
        super(worldServer.getChunkProvider(), EnumSkyBlock.BLOCK, ReflectionHelper.Safe.get(FIELD_LIGHT_ENGINE_STORAGE, base));

        this.base = base;
    }

    private int getLight(long position) {
        BlockPosition blockPosition = BlockPosition.fromLong(position);
        IBlockAccess blockAccess = this.a.b(blockPosition.getX() >> 4, blockPosition.getZ() >> 4);

        return blockAccess != null ? Math.max(b(blockPosition), blockAccess.h(blockPosition)) : 0;
    }

    @Override
    protected int b(long i, long j, int k) {
        if (i == Long.MAX_VALUE) {
            return k + 15 - getLight(j);
        }

        try {
            return (int) ReflectionHelper.Safe.invoke(base, METHOD_B, i, j, k);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    @Override
    protected void a(long l, int i, boolean b) {
        try {
            ReflectionHelper.Safe.invoke(base, METHOD_A, l, i, b);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    @Override
    protected int a(long i, long j, int k) {
        try {
            return (int) ReflectionHelper.Safe.invoke(base, METHOD_OTHER_A, i, j, k);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
    }

    @Override
    public void a(BlockPosition blockPosition, int lightLevel) {
        try {
            ReflectionHelper.Safe.invoke(c, METHOD_C);
            this.a(Long.MAX_VALUE, blockPosition.asLong(), 15 - lightLevel, true);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new LightUpdateFailedException(e);
        }
    }
}
