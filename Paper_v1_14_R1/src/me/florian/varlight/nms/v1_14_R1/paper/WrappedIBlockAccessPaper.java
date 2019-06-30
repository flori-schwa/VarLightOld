package me.florian.varlight.nms.v1_14_R1.paper;

import me.florian.varlight.nms.v1_14_R1.NmsAdapter_1_14_R1;
import net.minecraft.server.v1_14_R1.*;

import javax.annotation.Nullable;

public class WrappedIBlockAccessPaper implements IBlockAccess {

    private WorldServer wrappedWorld;
    private IBlockAccess wrappedBlockAccess;

    public WrappedIBlockAccessPaper(WorldServer wrappedWorld, IBlockAccess wrappedBlockAccess) {
        this.wrappedWorld = wrappedWorld;
        this.wrappedBlockAccess = wrappedBlockAccess;
    }

    public int h(BlockPosition position) {
        return NmsAdapter_1_14_R1.getLuminance(wrappedWorld, position, () -> IBlockAccess.super.h(position));
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition blockPosition) {
        return wrappedBlockAccess.getTileEntity(blockPosition);
    }

    @Override
    public IBlockData getTypeIfLoaded(BlockPosition blockPosition) {
        return wrappedBlockAccess.getTypeIfLoaded(blockPosition);
    }

    @Override
    public IBlockData getType(BlockPosition blockPosition) {
        return wrappedBlockAccess.getType(blockPosition);
    }

    @Override
    public Fluid getFluidIfLoaded(BlockPosition blockPosition) {
        return wrappedBlockAccess.getFluidIfLoaded(blockPosition);
    }

    @Override
    public Fluid getFluid(BlockPosition blockPosition) {
        return wrappedBlockAccess.getFluid(blockPosition);
    }
}
