package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.CollectionUtil;
import me.shawlaf.varlight.util.IntPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class RegionPersistor<L extends ICustomLightSource> {

//    private static int count;

    public final int regionX, regionZ;

    public final VLDBFile<L> file;
    private final Map<ChunkCoords, List<L>> chunkCache = new HashMap<>();

//    {
//        count++;
//    }

    public RegionPersistor(@NotNull File vldbRoot, int regionX, int regionZ, boolean deflated) throws IOException {
        Objects.requireNonNull(vldbRoot);

        if (!vldbRoot.exists()) {
            if (!vldbRoot.mkdir()) {
                throw new LightPersistFailedException("Could not create directory \"" + vldbRoot.getAbsolutePath() + "\"");
            }
        }

        if (!vldbRoot.isDirectory()) {
            throw new IllegalArgumentException(String.format("\"%s\" is not a directory!", vldbRoot.getAbsolutePath()));
        }

        this.regionX = regionX;
        this.regionZ = regionZ;

        File vldbFile = new File(vldbRoot, String.format(VLDBFile.FILE_NAME_FORMAT, regionX, regionZ));

        if (!vldbFile.exists()) {
            this.file = new VLDBFile<L>(vldbFile, regionX, regionZ, deflated) {
                @NotNull
                @Override
                protected L[] createArray(int size) {
                    return RegionPersistor.this.createArray(size);
                }

                @NotNull
                @Override
                protected L createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return RegionPersistor.this.createInstance(position, lightLevel, migrated, material);
                }
            };
        } else {
            this.file = new VLDBFile<L>(vldbFile, deflated) {
                @NotNull
                @Override
                protected L[] createArray(int size) {
                    return RegionPersistor.this.createArray(size);
                }

                @NotNull
                @Override
                protected L createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return RegionPersistor.this.createInstance(position, lightLevel, migrated, material);
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

    public boolean isChunkLoaded(@NotNull ChunkCoords chunkCoords) {
        Objects.requireNonNull(chunkCoords);

        synchronized (chunkCache) {
            return chunkCache.containsKey(chunkCoords);
        }
    }

    public void unloadChunk(@NotNull ChunkCoords chunkCoords) throws IOException {
        Objects.requireNonNull(chunkCoords);

        synchronized (chunkCache) {
            List<L> toUnload = chunkCache.remove(chunkCoords);

            if (toUnload == null) { // There was no mapping for the chunk
                return;
            }

            flushChunk(chunkCoords, toUnload);
        }
    }

    @NotNull
    public List<L> getCache(@NotNull ChunkCoords chunkCoords) {
        List<L> chunk;

        synchronized (chunkCache) {
            chunk = chunkCache.get(chunkCoords);
        }

        if (chunk == null) {
            return new ArrayList<>();
        }

        return Collections.unmodifiableList(chunk);
    }

    @Nullable
    public L getLightSource(@NotNull IntPosition position) throws IOException {
        Objects.requireNonNull(position);

        ChunkCoords chunkCoords = position.toChunkCoords();

        synchronized (chunkCache) {
            if (!chunkCache.containsKey(chunkCoords)) {
                loadChunk(chunkCoords);
            }

            for (L lightSource : chunkCache.get(chunkCoords)) {
                if (lightSource.getPosition().equals(position)) {
                    return lightSource;
                }
            }
        }

        return null;
    }

    public void put(@NotNull L lightSource) throws IOException {
        Objects.requireNonNull(lightSource);

        ChunkCoords chunkCoords = lightSource.getPosition().toChunkCoords();

        synchronized (chunkCache) {
            if (!chunkCache.containsKey(chunkCoords)) {
                loadChunk(chunkCoords);
            }

            putInternal(lightSource);
        }
    }

    @Deprecated
    public void removeLightSource(@NotNull IntPosition position) throws IOException {
        Objects.requireNonNull(position);

        ChunkCoords chunkCoords = position.toChunkCoords();

        synchronized (chunkCache) {
            if (!chunkCache.containsKey(chunkCoords)) {
                loadChunk(chunkCoords);
            }

            if (chunkCache.get(chunkCoords).removeIf(l -> l.getPosition().equals(position))) {
                List<L> chunk = chunkCache.get(chunkCoords);

                if (chunk.size() == 0) {
                    file.removeChunk(chunkCoords);
                } else {
                    file.editChunk(chunkCoords, chunk.toArray(createArray(0)));
                }
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

    public List<L> loadAll() throws IOException {
        synchronized (file) {
            synchronized (chunkCache) {
                for (ChunkCoords chunkCoords : chunkCache.keySet()) {
                    flushChunk(chunkCoords);
                }

                return file.readAll();
            }
        }
    }

    public boolean save() throws IOException {
        synchronized (file) {
            return file.save();
        }
    }

    private void flushChunk(ChunkCoords chunkCoords, List<L> lightData) throws IOException {
        synchronized (file) {
            if (lightData.size() == 0) {
                if (file.hasChunkData(chunkCoords)) {
                    file.removeChunk(chunkCoords);
                }

                return;
            }

            if (!file.hasChunkData(chunkCoords)) {
                file.insertChunk(lightData.toArray(createArray(0)));
            } else {
                file.editChunk(chunkCoords, lightData.toArray(createArray(0)));
            }
        }
    }

    private void flushChunk(ChunkCoords chunkCoords) throws IOException {
        synchronized (chunkCache) {
            flushChunk(chunkCoords, chunkCache.get(chunkCoords));
        }
    }

    private void putInternal(L lightSource) {
        Objects.requireNonNull(lightSource);

        ChunkCoords chunkCoords = lightSource.getPosition().toChunkCoords();

        synchronized (chunkCache) {
            List<L> list = chunkCache.get(chunkCoords);

            if (list == null) {
                throw new IllegalArgumentException("No Data present for chunk");
            }

            list.removeIf(l -> l.getPosition().equals(lightSource.getPosition()));

            if (lightSource.getCustomLuminance() > 0) {
                list.add(lightSource);
            }
        }
    }

    @NotNull
    protected abstract L[] createArray(int size);

    @NotNull
    protected abstract L createInstance(IntPosition position, int lightLevel, boolean migrated, String material);

    public void unload() {
        chunkCache.clear();

        file.unload();
    }

//    @Override
//    protected void finalize() throws Throwable {
//        count--;
//        super.finalize();
//    }
}