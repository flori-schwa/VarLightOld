package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;
import static me.shawlaf.varlight.persistence.vldb.VLDBUtil.*;

public abstract class VLDBFile<L extends ICustomLightSource> {

    private final Object lock = new Object();

    public static String FILE_NAME_FORMAT = "r%d.%d.vldb";

    public final File file;
    public byte[] fileContents;

    private final int regionX, regionZ;
    private Map<ChunkCoords, Integer> offsetTable;

    private boolean modified = false;

    public static String getFileName(ICustomLightSource[] region) {
        final int rx = region[0].getPosition().getRegionX();
        final int rz = region[0].getPosition().getRegionZ();

        if (!allLightSourcesInRegion(rx, rz, region)) {
            throw new IllegalArgumentException("Not all light sources are in the same region!");
        }

        return String.format(FILE_NAME_FORMAT, rx, rz);
    }

    public static boolean allLightSourcesInRegion(int rx, int rz, ICustomLightSource[] lightSources) {
        for (ICustomLightSource iCustomLightSource : lightSources) {
            IntPosition pos = iCustomLightSource.getPosition();

            if (pos.getRegionX() != rx || pos.getRegionZ() != rz) {
                return false;
            }
        }

        return true;
    }


    public VLDBFile(@NotNull File file, int regionX, int regionZ) throws IOException {
        this.file = requireNonNull(file);

        synchronized (lock) {
            this.regionX = regionX;
            this.regionZ = regionZ;

            this.offsetTable = new HashMap<>();

            Pair<ByteArrayOutputStream, VLDBOutputStream> out = outToMemory(sizeofHeader(0));

            out.item2.writeHeader(regionX, regionZ, offsetTable);
            out.item2.close();

            this.fileContents = out.item1.toByteArray();

            this.modified = true;
        }
    }

    public VLDBFile(@NotNull File file) throws IOException {
        this.file = requireNonNull(file);

        synchronized (lock) {
            readFileFully();

            VLDBInputStream headerReader = in();

            this.offsetTable = headerReader.readHeader(
                    this.regionX = headerReader.readInt32(),
                    this.regionZ = headerReader.readInt32()
            );

            headerReader.close();
        }
    }

    public Map<ChunkCoords, Integer> getOffsetTable() {
        return Collections.unmodifiableMap(offsetTable);
    }

    @NotNull
    public L[] readChunk(int chunkX, int chunkZ) throws IOException {
        return readChunk(new ChunkCoords(chunkX, chunkZ));
    }

    @NotNull
    public L[] readChunk(@NotNull ChunkCoords chunkCoords) throws IOException {
        requireNonNull(chunkCoords);

        if (chunkCoords.getRegionX() != regionX || chunkCoords.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("%s not in region %d %d", chunkCoords.toString(), regionX, regionZ));
        }

        if (!offsetTable.containsKey(chunkCoords)) {
            return createArray(0);
        }

        synchronized (lock) {
            try (VLDBInputStream in = in(offsetTable.get(chunkCoords))) {
                return in.readChunk(regionX, regionZ, this::createArray, this::createInstance).item2;
            }
        }
    }

    @NotNull
    public List<L> readAll() throws IOException {
        try (VLDBInputStream in = in()) { // Skip Header
            return in.readAll(this::createArray, this::createInstance);
        }
    }

//    @Deprecated
//    public void write(L[] region) throws IOException {
//        synchronized (lock) {
//            VLDBOutputStream outputStream = out();
//
//            outputStream.write(region);
//
//            outputStream.close();
//
//            readFileFully();
//
//            rereadHeader();
//        }
//    }

    public boolean hasChunkData(int cx, int cz) {
        return hasChunkData(new ChunkCoords(cx, cz));
    }

    public boolean hasChunkData(ChunkCoords chunkCoords) {
        synchronized (lock) {
            return offsetTable.containsKey(chunkCoords);
        }
    }

    public void insertChunk(@NotNull L[] chunk) throws IOException {
        requireNonNull(chunk);

        if (chunk.length == 0) {
            throw new IllegalArgumentException("Array may not be empty!");
        }

        final int cx = chunk[0].getPosition().getChunkX();
        final int cz = chunk[0].getPosition().getChunkZ();

        for (int i = 1; i < chunk.length; i++) {
            IntPosition pos = chunk[i].getPosition();

            if (pos.getChunkX() != cx || pos.getChunkZ() != cz) {
                throw new IllegalArgumentException("Not all Light sources are in the same chunk!");
            }
        }

        if ((cx >> 5) != regionX || (cz >> 5) != regionZ) {
            throw new IllegalArgumentException(String.format("Chunk %d %d not in region %d %d", cx, cz, regionX, regionZ));
        }

        synchronized (lock) {
            final ChunkCoords chunkCoords = new ChunkCoords(cx, cz);

            if (hasChunkData(chunkCoords)) {
                throw new IllegalStateException("Chunk already in this file!");
            }

            Map<ChunkCoords, Integer> newOffsetTable = new HashMap<>();

            for (ChunkCoords key : offsetTable.keySet()) {
                newOffsetTable.put(key, offsetTable.get(key) + SIZEOF_OFFSET_TABLE_ENTRY);
            }

            newOffsetTable.put(chunkCoords, fileContents.length + SIZEOF_OFFSET_TABLE_ENTRY);

            Pair<ByteArrayOutputStream, VLDBOutputStream> out = outToMemory();

            out.item2.writeChunk(chunkCoords, chunk);
            out.item2.close();

            final int oldHeaderSize = sizeofHeader(offsetTable.keySet().size());

            final byte[] append = out.item1.toByteArray();
            Pair<ByteArrayOutputStream, VLDBOutputStream> newFileOut = outToMemory(fileContents.length + SIZEOF_OFFSET_TABLE_ENTRY + append.length);

            newFileOut.item2.writeHeader(regionX, regionZ, newOffsetTable);
            newFileOut.item2.write(fileContents, oldHeaderSize, fileContents.length - oldHeaderSize);
            newFileOut.item2.write(append);

            newFileOut.item2.close();

            this.offsetTable = newOffsetTable;
            this.fileContents = newFileOut.item1.toByteArray();

            this.modified = true;
        }
    }

    public void editChunk(@NotNull ChunkCoords coords, @NotNull L[] data) throws IOException {
        requireNonNull(data);

        if (data.length == 0) {
            throw new IllegalArgumentException("Array may not be empty!");
        }

        final int cx = coords.x;
        final int cz = coords.z;

        for (int i = 0; i < data.length; i++) {
            IntPosition pos = data[i].getPosition();

            if (pos.getChunkX() != cx || pos.getChunkZ() != cz) {
                throw new IllegalArgumentException("Not all Light sources are in the same chunk!");
            }
        }

        if (coords.getRegionX() != regionX || coords.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("Chunk %d %d not in region %d %d", coords.x, coords.z, regionX, regionZ));
        }

        synchronized (lock) {
            if (!hasChunkData(coords)) {
                throw new IllegalArgumentException("Cannot edit chunk that is not already present");
            }

            final int newChunkSize = sizeofChunk(data);
            final int oldChunkSize = sizeofChunk(readChunk(coords));

            final int targetChunkOffset = offsetTable.get(coords);

            if (newChunkSize == oldChunkSize) {
                // Header does not change, neither does total size

                Pair<ByteArrayOutputStream, VLDBOutputStream> newFileOut = outToMemory(fileContents.length);

                newFileOut.item2.write(fileContents, 0, targetChunkOffset);
                newFileOut.item2.writeChunk(coords, data);
                newFileOut.item2.write(fileContents, targetChunkOffset + oldChunkSize, fileContents.length - (targetChunkOffset + oldChunkSize));

                newFileOut.item2.close();

                this.fileContents = newFileOut.item1.toByteArray();
            } else {
                // Size of Header stays the same
                // All chunks BEFORE the target chunk will keep the same offset
                // All chunk AFTER the target chunk will have their offset increased by (newChunkSize - oldChunkSize)

                final Map<ChunkCoords, Integer> newOffsetTable = new HashMap<>();

                for (Map.Entry<ChunkCoords, Integer> entry : offsetTable.entrySet()) {
                    if (coords.equals(entry.getKey())) {
                        newOffsetTable.put(entry.getKey(), entry.getValue());
                    } else if (entry.getValue() < targetChunkOffset) {
                        newOffsetTable.put(entry.getKey(), entry.getValue());
                    } else {
                        newOffsetTable.put(entry.getKey(), entry.getValue() + (newChunkSize - oldChunkSize));
                    }
                }

                final int headerLength = sizeofHeader(newOffsetTable.keySet().size());

                Pair<ByteArrayOutputStream, VLDBOutputStream> newFileOut = outToMemory(fileContents.length + (newChunkSize - oldChunkSize));

                newFileOut.item2.writeHeader(regionX, regionZ, newOffsetTable);
                newFileOut.item2.write(fileContents, headerLength, (targetChunkOffset - headerLength));
                newFileOut.item2.writeChunk(coords, data);
                newFileOut.item2.write(fileContents, targetChunkOffset + oldChunkSize, fileContents.length - (targetChunkOffset + oldChunkSize));

                newFileOut.item2.close();

                this.offsetTable = newOffsetTable;
                this.fileContents = newFileOut.item1.toByteArray();
            }

            this.modified = true;
        }
    }

    public void removeChunk(@NotNull ChunkCoords coords) throws IOException {
        requireNonNull(coords);

        if (coords.getRegionX() != regionX || coords.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("Chunk %d %d not in region %d %d", coords.x, coords.z, regionX, regionZ));
        }

        synchronized (lock) {
            if (!hasChunkData(coords)) {
                throw new IllegalStateException("Chunk not contained within this File!");
            }

            // All chunks BEFORE the target chunk will have their offset reduced by 6 (OFFSET_TABLE_ENTRY_SIZE)
            // All chunks AFTER the target chunk will have their offset reduced by 6 + sizeof(targetchunk)

            final int targetChunkOffset = offsetTable.get(coords);
            final int targetChunkSize = sizeofChunk(readChunk(coords));
            final Map<ChunkCoords, Integer> newOffsetTable = new HashMap<>();

            for (Map.Entry<ChunkCoords, Integer> entry : offsetTable.entrySet()) {
                if (coords.equals(entry.getKey())) {
                    continue;
                }

                if (entry.getValue() < targetChunkOffset) {
                    newOffsetTable.put(entry.getKey(), entry.getValue() - SIZEOF_OFFSET_TABLE_ENTRY);
                } else {
                    newOffsetTable.put(entry.getKey(), entry.getValue() - (SIZEOF_OFFSET_TABLE_ENTRY + targetChunkSize));
                }
            }

            final int oldHeaderSize = sizeofHeader(offsetTable.keySet().size());
            final int newHeaderSize = sizeofHeader(newOffsetTable.keySet().size());

            final Pair<ByteArrayOutputStream, VLDBOutputStream> newFileOut = outToMemory(newHeaderSize + (fileContents.length - oldHeaderSize - targetChunkSize));

            newFileOut.item2.writeHeader(regionX, regionZ, newOffsetTable);
            newFileOut.item2.write(fileContents, oldHeaderSize, (targetChunkOffset - oldHeaderSize));
            newFileOut.item2.write(fileContents, (targetChunkOffset + targetChunkSize), (fileContents.length - (targetChunkOffset + targetChunkSize)));

            newFileOut.item2.close();

            this.offsetTable = newOffsetTable;
            this.fileContents = newFileOut.item1.toByteArray();

            this.modified = true;
        }
    }

    public boolean isModified() {
        return modified;
    }

    public void save() throws IOException {
        synchronized (lock) {
            if (!modified) {
                return;
            }

            try (VLDBOutputStream out = out()) {
                out.write(fileContents);
            }

            modified = false;
        }
    }

    @NotNull
    private VLDBInputStream in(int offset) {
        return new VLDBInputStream(new ByteArrayInputStream(fileContents, offset, fileContents.length - offset));
    }

    @NotNull
    private VLDBInputStream in() {
        return new VLDBInputStream(new ByteArrayInputStream(fileContents));
    }

    @NotNull
    private VLDBOutputStream out() throws IOException {
        return new VLDBOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
    }

    @NotNull
    private Pair<ByteArrayOutputStream, VLDBOutputStream> outToMemory() {
        return outToMemory(32);
    }

    @NotNull
    private Pair<ByteArrayOutputStream, VLDBOutputStream> outToMemory(int size) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);

        return new Pair<>(baos, new VLDBOutputStream(baos));
    }

    private void readFileFully() throws IOException {
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(file));
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.toIntExact(file.length()));

        byte[] buffer = new byte[1024];
        int read = 0;

        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }

        this.fileContents = out.toByteArray();

        in.close();
    }

    private void rereadHeader() throws IOException {
        VLDBInputStream headerReader = in();

        int readRx = headerReader.readInt32();
        int readRz = headerReader.readInt32();

        if (readRx != regionX || readRz != regionZ) {
            throw new RuntimeException(String.format("Region information in header changed? (was: %d %d, is: %d %d)", regionX, regionZ, readRx, readRz)); // TODO Custom Exception?
        }

        this.offsetTable = headerReader.readHeader(this.regionX, this.regionZ);

        headerReader.close();
    }

    @NotNull
    protected abstract L[] createArray(int size);

    @NotNull
    protected abstract L createInstance(IntPosition position, int lightLevel, boolean migrated, String material);

    public boolean delete() {
        synchronized (lock) {
            return file.delete();
        }
    }
}
