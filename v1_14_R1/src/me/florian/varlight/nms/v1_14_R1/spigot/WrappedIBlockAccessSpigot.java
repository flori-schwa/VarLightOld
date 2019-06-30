package me.florian.varlight.nms.v1_14_R1.spigot;

import me.florian.varlight.nms.v1_14_R1.NmsAdapter_1_14_R1;
import net.minecraft.server.v1_14_R1.*;

import javax.annotation.Nullable;

public class WrappedIBlockAccessSpigot implements IBlockAccess {

    private WorldServer wrappedWorld;
    private IBlockAccess wrappedBlockAccess;

    public WrappedIBlockAccessSpigot(WorldServer wrappedWorld, IBlockAccess wrappedBlockAccess) {
        this.wrappedWorld = wrappedWorld;
        this.wrappedBlockAccess = wrappedBlockAccess;
    }

    @Override
    public int h(BlockPosition position) {
        return NmsAdapter_1_14_R1.getLuminance(wrappedWorld, position, () -> IBlockAccess.super.h(position));
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition blockPosition) {
        return wrappedBlockAccess.getTileEntity(blockPosition);
    }

    @Override
    public IBlockData getType(BlockPosition blockPosition) {
        return wrappedBlockAccess.getType(blockPosition);
    }

    @Override
    public Fluid getFluid(BlockPosition blockPosition) {
        return wrappedBlockAccess.getFluid(blockPosition);
    }
}
