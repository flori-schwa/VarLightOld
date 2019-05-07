package me.florian.varlight.nms;

import net.minecraft.server.v1_14_R1.*;

import javax.annotation.Nullable;

public class WrappedIBlockAccess implements IBlockAccess {

    private IBlockAccess wrapped;

    public WrappedIBlockAccess(IBlockAccess wrapped) {
        this.wrapped = wrapped;
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition blockPosition) {
        return wrapped.getTileEntity(blockPosition);
    }

    @Override
    public IBlockData getType(BlockPosition blockPosition) {
        return wrapped.getType(blockPosition);
    }

    @Override
    public Fluid getFluid(BlockPosition blockPosition) {
        return wrapped.getFluid(blockPosition);
    }

    @Override
    public int h(BlockPosition position) {
        int emitting = wrapped.h(position);
        int customLightSource = 0;// TODO get light level from persistence

        return Math.max(emitting, customLightSource);
    }
}
