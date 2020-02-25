package me.shawlaf.varlight.spigot.nms.wrappers;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// Normally, WorldServer.getChunkProvider gets passed as the ILightAccess parameter to the constructor of LightEngineThreaded
public class WrappedILightAccess implements ILightAccess, Listener {

    private final Map<ChunkCoords, IBlockAccess> wrapped = Collections.synchronizedMap(new HashMap<>());

    private final VarLightPlugin plugin;
    private final WorldServer world;

    public WrappedILightAccess(VarLightPlugin plugin, WorldServer world) {
        this.plugin = plugin;
        this.world = world;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private int getCustomLuminance(IBlockAccess blockAccess, BlockPosition bPos) {
        int vanilla = blockAccess.getType(bPos).h();

        WorldLightSourceManager manager = plugin.getManager(world.getWorld());

        if (manager == null) {
            return vanilla;
        }

        return manager.getCustomLuminance(new IntPosition(bPos.getX(), bPos.getY(), bPos.getZ()), vanilla);
    }

    private IBlockAccess createProxy(ChunkCoords chunkCoords) {
        IBlockAccess toWrap = getWrapped().c(chunkCoords.x, chunkCoords.z);

        if (toWrap == null) {
            return null;
        }

        return (IBlockAccess) Proxy.newProxyInstance(
                IBlockAccess.class.getClassLoader(),
                new Class[]{IBlockAccess.class},

                (proxy, method, args) -> {
                    if (method.getName().equals("h")) {
                        return getCustomLuminance(toWrap, (BlockPosition) args[0]);
                    }

                    return method.invoke(toWrap, args);
                }
        );
    }

    @Nullable
    @Override
    public IBlockAccess c(int i, int i1) {
        ChunkCoords chunkCoords = new ChunkCoords(i, i1);

        synchronized (wrapped) {
            IBlockAccess res = createProxy(chunkCoords);

            if (res != null && !wrapped.containsKey(chunkCoords)) {
                wrapped.put(chunkCoords, createProxy(chunkCoords));
            }
        }

        return wrapped.get(chunkCoords);
    }

    @Override
    public void a(EnumSkyBlock var0, SectionPosition var1) {
        getWrapped().a(var0, var1);
    }

    @Override
    public IBlockAccess getWorld() {
        return getWrapped().getWorld();
    }

    private ILightAccess getWrapped() {
        return world.getChunkProvider();
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        World world = e.getWorld();

        if (!world.getUID().equals(this.world.getWorld().getUID())) {
            return;
        }

        Chunk chunk = e.getChunk();

        wrapped.remove(new ChunkCoords(chunk.getX(), chunk.getZ()));
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        wrapped.clear();
        HandlerList.unregisterAll(this);
    }
}
