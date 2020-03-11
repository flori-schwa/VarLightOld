package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.util.ChunkCoords;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.*;

public class LightUpdateScheduler {

    private final Map<UUID, Set<ChunkCoords>> chunksToUpdate = new HashMap<>(), blocksToUpdate = new HashMap<>();

    public void start(VarLightPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin,
                () -> {
                    synchronized (LightUpdateScheduler.this) {
                        for (World world : Bukkit.getWorlds()) {
                            if (!plugin.hasManager(world)) {
                                continue;
                            }

                            Set<ChunkCoords> updateBlocks = blocksToUpdate.remove(world.getUID());
                            Set<ChunkCoords> updateChunks = chunksToUpdate.remove(world.getUID());

                            if (updateBlocks != null) {
                                for (ChunkCoords chunkToUpdateBlocksIn : updateBlocks) {
                                    plugin.getNmsAdapter().updateBlocks(world, chunkToUpdateBlocksIn);
                                }
                            }

                            if (updateChunks != null) {
                                for (ChunkCoords chunkToUpdate : updateChunks) {
                                    plugin.getNmsAdapter().updateChunk(world, chunkToUpdate);
                                }
                            }
                        }
                    }

                },
                1L, 1L
        );
    }

    public void enqueueChunks(boolean updateChunk, boolean updateBlocks, World world, ChunkCoords... chunks) {
        enqueueChunks(updateChunk, updateBlocks, world, Arrays.asList(chunks));
    }

    public void enqueueChunks(boolean updateChunk, boolean updateBlocks, World world, Collection<ChunkCoords> chunks) {
        if (!updateBlocks && !updateChunk) {
            return; // NOP
        }

        synchronized (this) {
            if (updateBlocks) {
                if (!blocksToUpdate.containsKey(world.getUID())) {
                    blocksToUpdate.put(world.getUID(), new HashSet<>());
                }

                blocksToUpdate.get(world.getUID()).addAll(chunks);
            }

            if (updateChunk) {
                if (!chunksToUpdate.containsKey(world.getUID())) {
                    chunksToUpdate.put(world.getUID(), new HashSet<>());
                }

                chunksToUpdate.get(world.getUID()).addAll(chunks);
            }
        }
    }


}
