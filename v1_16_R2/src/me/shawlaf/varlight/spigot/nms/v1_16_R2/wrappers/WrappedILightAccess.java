package me.shawlaf.varlight.spigot.nms.v1_16_R2.wrappers;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_16_R2.*;
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

public class WrappedILightAccess implements ILightAccess, Listener {

    private final Map<ChunkCoords, IChunkAccess> proxies = Collections.synchronizedMap(new HashMap<>());

    private final VarLightPlugin plugin;
    private final WorldServer world;

    public WrappedILightAccess(VarLightPlugin plugin, WorldServer world) {
        this.plugin = plugin;
        this.world = world;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private int getCustomLuminance(IChunkAccess blockAccess, BlockPosition bPos) {
        int vanilla = blockAccess.getType(bPos).f();

        WorldLightSourceManager manager = plugin.getManager(world.getWorld());

        if (manager == null) {
            return vanilla;
        }

        return manager.getCustomLuminance(new IntPosition(bPos.getX(), bPos.getY(), bPos.getZ()), vanilla);
    }

    private IChunkAccess createProxy(ChunkCoords chunkCoords) {
        IChunkAccess toWrap = (IChunkAccess) getWrapped().c(chunkCoords.x, chunkCoords.z);

        if (toWrap == null) {
            return null;
        }

        return (IChunkAccess) Proxy.newProxyInstance(
                IChunkAccess.class.getClassLoader(),
                new Class[]{IChunkAccess.class},

                (proxy, method, args) -> {
                    if (method.getName().equals("g")) {
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

        synchronized (proxies) {
            IChunkAccess res = createProxy(chunkCoords);

            if (res != null && !proxies.containsKey(chunkCoords)) {
                proxies.put(chunkCoords, res);
            }
        }

        return proxies.get(chunkCoords);
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
        // Normally, WorldServer.getChunkProvider gets passed as the ILightAccess parameter to the constructor of LightEngineThreaded
        return world.getChunkProvider();
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        World world = e.getWorld();

        if (!world.getUID().equals(this.world.getWorld().getUID())) {
            return;
        }

        Chunk chunk = e.getChunk();

        proxies.remove(new ChunkCoords(chunk.getX(), chunk.getZ()));
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        proxies.clear();
        HandlerList.unregisterAll(this);
    }
}
