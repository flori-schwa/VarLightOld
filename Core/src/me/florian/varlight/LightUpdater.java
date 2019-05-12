package me.florian.varlight;

import me.florian.varlight.util.IntPosition;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public interface LightUpdater {

    void setLight(Location location, int lightLevel);

    default int getChunkBitMask(Location location) {
        return getChunkBitMask(location.getBlockY() / 16);
    }

    default int getChunkBitMask(int sectionY) {
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
