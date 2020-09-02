package me.shawlaf.varlight.spigot.updater;

import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface ILightUpdater {

    void setLight(World world, IntPosition position, int lightLevel);

    void setLight(Location location, int lightLevel);

    CompletableFuture<Void> setAndUpdateLight(World world, IntPosition position, int lightLevel);

    CompletableFuture<Void> setAndUpdateLight(Location location, int lightLevel);

    CompletableFuture<Void> updateLight(World world, IntPosition position);

    CompletableFuture<Void> updateLight(Location location);

    CompletableFuture<Void> updateLight(World world, Collection<IntPosition> positions);

    CompletableFuture<Void> recalculateChunk(Chunk chunk);

    CompletableFuture<Void> recalculateChunk(World world, ChunkCoords chunkCoords);

    /**
     * Send light update packets in a 3x3 chunk radius around the center
     *
     * @param world The world containing the center chunk
     * @param center The center location of the block
     */
    void sendLightUpdates(World world, ChunkCoords center);

    /**
     * Runs lightChunk
     *
     * @param world The world containing the chunk
     * @param chunkCoords The coordinates of the chunk
     * @return
     */
    CompletableFuture<Void> updateChunk(World world, ChunkCoords chunkCoords);

}
