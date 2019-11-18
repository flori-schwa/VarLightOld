package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.CollectionUtil;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionPersistor {

    public final int regionX, regionZ;
    private final World world;
    private final VarLightPlugin plugin;

    public final VLDBFile<PersistentLightSource> file;
    private final Map<ChunkCoords, List<PersistentLightSource>> chunkCache = new HashMap<>();

    public RegionPersistor(VarLightPlugin plugin, File vldbRoot, World world, int regionX, int regionZ) throws IOException {
        Objects.requireNonNull(vldbRoot);

        this.world = Objects.requireNonNull(world);
        this.plugin = Objects.requireNonNull(plugin);

        if (!vldbRoot.exists()) {
            vldbRoot.mkdir();
        }

        if (!vldbRoot.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not a directory!", vldbRoot.getAbsolutePath()));
        }

        this.regionX = regionX;
        this.regionZ = regionZ;

        File vldbFile = new File(vldbRoot, String.format(VLDBFile.FILE_NAME_FORMAT, regionX, regionZ));

        if (!vldbFile.exists()) {
            this.file = new VLDBFile<PersistentLightSource>(vldbFile, regionX, regionZ) {
                @NotNull
                @Override
                protected PersistentLightSource[] createArray(int size) {
                    return new PersistentLightSource[size];
                }

                @NotNull
                @Override
                protected PersistentLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return new PersistentLightSource(position, Material.valueOf(material), migrated, world, plugin, lightLevel);
                }
            };
        } else {
            this.file = new VLDBFile<PersistentLightSource>(vldbFile) {
                @NotNull
                @Override
                protected PersistentLightSource[] createArray(int size) {
                    return new PersistentLightSource[size];
                }

                @NotNull
                @Override
                protected PersistentLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return new PersistentLightSource(position, Material.valueOf(material), migrated, world, plugin, lightLevel);
                }
            };
        }
    }

    public void loadChunk(@NotNull ChunkCoords chunkCoords) throws IOException {
        Objects.requireNonNull(chunkCoords);

        synchronized (chunkCache) {
            synchronized (file) {
                chunkCache.put(chunkCoords, CollectionUtil.toList(file.readChunk(chunkCoords)));
            }
        }
    }

    public void unloadChunk(ChunkCoords chunkCoords) throws IOException {
        Objects.requireNonNull(chunkCoords);

        synchronized (chunkCache) {
            List<PersistentLightSource> toUnload = chunkCache.remove(chunkCoords);

            if (toUnload == null) { // There was no mapping for the chunk
                return;
            }

            flushChunk(chunkCoords, toUnload);
        }
    }

    public void put(PersistentLightSource lightSource) throws IOException {
        ChunkCoords chunkCoords = lightSource.getPosition().toChunkCoords();

        synchronized (chunkCache) {
            if (chunkCache.containsKey(chunkCoords)) {
                putInternal(lightSource);
            } else {
                loadChunk(chunkCoords);
                putInternal(lightSource);
                unloadChunk(chunkCoords);
            }
        }
    }

    public void flushAll() throws IOException {
        synchronized (chunkCache) {
            synchronized (file) {
                for (ChunkCoords key : chunkCache.keySet()) {
                    flushChunk(key);
                }
            }
        }
    }

    public List<ChunkCoords> getAffectedChunks() {
        synchronized (file) {
            return new ArrayList<>(file.getOffsetTable().keySet());
        }
    }

    private void flushChunk(ChunkCoords chunkCoords, List<PersistentLightSource> lightData) throws IOException {
        synchronized (file) {
            if (lightData.size() == 0) {
                if (file.hasChunkData(chunkCoords)) {
                    file.removeChunk(chunkCoords);
                }

                return;
            }

            if (!file.hasChunkData(chunkCoords)) {
                file.insertChunk(lightData.toArray(new PersistentLightSource[0]));
            } else {
                file.editChunk(chunkCoords, lightData.toArray(new PersistentLightSource[0]));
            }
        }
    }

    private void flushChunk(ChunkCoords chunkCoords) throws IOException {
        synchronized (chunkCache) {
            flushChunk(chunkCoords, chunkCache.get(chunkCoords));
        }
    }

    private void putInternal(PersistentLightSource lightSource) {
        ChunkCoords chunkCoords = lightSource.getPosition().toChunkCoords();

        synchronized (chunkCache) {
            List<PersistentLightSource> list = chunkCache.get(chunkCoords);

            if (list == null) {
                throw new IllegalArgumentException("No Data present for chunk");
            }

            list.removeIf(l -> l.getPosition().equals(lightSource.getPosition()));
            list.add(lightSource);
        }
    }

    public List<PersistentLightSource> loadAll() throws IOException {
        synchronized (file) {
            synchronized (chunkCache) {
                for (ChunkCoords chunkCoords : chunkCache.keySet()) {
                    flushChunk(chunkCoords);
                }

                return file.readAll();
            }
        }
    }

    public void save() throws IOException {
        synchronized (file) {
            file.save();
        }
    }

    @Nullable
    public PersistentLightSource getLightSource(IntPosition position) {
        ChunkCoords chunkCoords = position.toChunkCoords();

        synchronized (chunkCache) {

            if (!chunkCache.containsKey(chunkCoords)) {
                return null;
            }

            for (PersistentLightSource lightSource : chunkCache.get(chunkCoords)) {
                if (lightSource.getPosition().equals(position)) {
                    return lightSource;
                }
            }
        }

        return null;
    }
}