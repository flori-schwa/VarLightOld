package me.florian.varlight.nms.v1_14_R1.paper;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import me.florian.varlight.nms.v1_14_R1.NmsAdapter_1_14_R1;
import net.minecraft.server.v1_14_R1.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class WrappedIChunkAccessPaper implements IChunkAccess {

    private IChunkAccess wrapped;
    private WorldServer worldServer;

    public WrappedIChunkAccessPaper(WorldServer worldServer, IChunkAccess wrapped) {
        this.worldServer = worldServer;
        this.wrapped = wrapped;
    }

    // Custom Behavior

    @Override
    public Stream<BlockPosition> m() {
        return StreamSupport.stream(BlockPosition.b(getPos().d(), 0, getPos().e(), getPos().f(), 255, getPos().g()).spliterator(), false).filter(pos -> this.h(pos) > 0);
    }

    @Override
    public int h(BlockPosition pos) {
        return NmsAdapter_1_14_R1.getLuminance(worldServer, pos, () -> IChunkAccess.super.h(pos));
    }

    // region Wrapped Behaviour
    @Nullable
    @Override
    public IBlockData setType(BlockPosition blockPosition, IBlockData iBlockData, boolean b) {
        return wrapped.setType(blockPosition, iBlockData, b);
    }

    @Override
    public void setTileEntity(BlockPosition blockPosition, TileEntity tileEntity) {
        wrapped.setTileEntity(blockPosition, tileEntity);
    }

    @Override
    public void a(Entity entity) {
        wrapped.a(entity);
    }

    @Override
    public Set<BlockPosition> c() {
        return wrapped.c();
    }

    @Override
    public ChunkSection[] getSections() {
        return wrapped.getSections();
    }

    @Nullable
    @Override
    public LightEngine e() {
        return wrapped.e();
    }

    @Override
    public Collection<Map.Entry<HeightMap.Type, HeightMap>> f() {
        return wrapped.f();
    }

    @Override
    public void a(HeightMap.Type type, long[] longs) {
        wrapped.a(type, longs);
    }

    @Override
    public HeightMap b(HeightMap.Type type) {
        return wrapped.b(type);
    }

    @Override
    public int a(HeightMap.Type type, int i, int i1) {
        return wrapped.a(type, i, i1);
    }

    @Override
    public ChunkCoordIntPair getPos() {
        return wrapped.getPos();
    }

    @Override
    public void setLastSaved(long l) {
        wrapped.setLastSaved(l);
    }

    @Override
    public Map<String, StructureStart> h() {
        return wrapped.h();
    }

    @Override
    public void a(Map<String, StructureStart> map) {
        wrapped.a(map);
    }

    @Override
    public BiomeBase[] getBiomeIndex() {
        return wrapped.getBiomeIndex();
    }

    @Override
    public void setNeedsSaving(boolean b) {
        wrapped.setNeedsSaving(b);
    }

    @Override
    public boolean isNeedsSaving() {
        return wrapped.isNeedsSaving();
    }

    @Override
    public ChunkStatus getChunkStatus() {
        return wrapped.getChunkStatus();
    }

    @Override
    public void removeTileEntity(BlockPosition blockPosition) {
        wrapped.removeTileEntity(blockPosition);
    }

    @Override
    public void a(LightEngine lightEngine) {
        wrapped.a(lightEngine);
    }

    @Override
    public ShortList[] l() {
        return wrapped.l();
    }

    @Override
    public TickList<Block> n() {
        return wrapped.n();
    }

    @Override
    public TickList<FluidType> o() {
        return wrapped.o();
    }

    @Override
    public ChunkConverter p() {
        return wrapped.p();
    }

    @Override
    public void b(long l) {
        wrapped.b(l);
    }

    @Override
    public long q() {
        return wrapped.q();
    }

    @Override
    public boolean r() {
        return wrapped.r();
    }

    @Override
    public void b(boolean b) {
        wrapped.b(b);
    }

    @Nullable
    @Override
    public StructureStart a(String s) {
        return wrapped.a(s);
    }

    @Override
    public void a(String s, StructureStart structureStart) {
        wrapped.a(s, structureStart);
    }

    @Override
    public LongSet b(String s) {
        return wrapped.b(s);
    }

    @Override
    public void a(String s, long l) {
        wrapped.a(s, l);
    }

    @Override
    public Map<String, LongSet> v() {
        return wrapped.v();
    }

    @Override
    public void b(Map<String, LongSet> map) {
        wrapped.b(map);
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition blockPosition) {
        return wrapped.getTileEntity(blockPosition);
    }

    @Override
    public IBlockData getTypeIfLoaded(BlockPosition blockPosition) {
        return wrapped.getTypeIfLoaded(blockPosition);
    }

    @Override
    public IBlockData getType(BlockPosition blockPosition) {
        return wrapped.getType(blockPosition);
    }

    @Override
    public Fluid getFluidIfLoaded(BlockPosition blockPosition) {
        return wrapped.getFluidIfLoaded(blockPosition);
    }

    @Override
    public Fluid getFluid(BlockPosition blockPosition) {
        return wrapped.getFluid(blockPosition);
    }

    @Nullable
    @Override
    public NBTTagCompound i(BlockPosition blockPosition) {
        return wrapped.i(blockPosition);
    }

    @Nullable
    @Override
    public NBTTagCompound j(BlockPosition blockPosition) {
        return wrapped.j(blockPosition);
    }

    //endregion
}
