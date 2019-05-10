package me.florian.varlight;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public interface LightUpdater {

    void setLight(Location location, int lightLevel);

    default int getChunkBitMask(Location location) {
        int sectionY = location.getBlockY() / 16;
        int mask = 1 << sectionY;

        if (sectionY == 0) {
            return mask | 2;
        }

        if (sectionY == 15) {
            return mask | 0x4000;
        }

        return mask | (1 << (sectionY - 1)) | (1 << (sectionY + 1));
    }

    default List<Chunk> getChunksToUpdate(Location location) {
        return getChunksToUpdate(new IntPosition(location), location.getWorld());
    }

    default List<Chunk> getChunksToUpdate(IntPosition location, World world) {
        int chunkX = location.getChunkX();
        int chunkZ = location.getChunkZ();

        List<Chunk> chunksToUpdate = new ArrayList<>();

        for (int dx = - 1; dx <= 1; dx++) {
            for (int dz = - 1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;

                if (! world.isChunkLoaded(x, z)) {
                    continue;
                }

                chunksToUpdate.add(world.getChunkAt(x, z));
            }
        }
        return chunksToUpdate;
    }
}
