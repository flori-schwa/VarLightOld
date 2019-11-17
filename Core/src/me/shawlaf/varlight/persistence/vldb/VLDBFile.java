package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;
import static me.shawlaf.varlight.persistence.vldb.VLDBOutputStream.HEADER_SIZE_WITHOUT_OFFSET_TABLE;
import static me.shawlaf.varlight.persistence.vldb.VLDBOutputStream.OFFSET_TABLE_ENTRY_SIZE;

public abstract class VLDBFile<L extends ICustomLightSource> {

    private final Object lock = new Object();

    public final File file;

    private final int regionX, regionZ;
    private byte[] fileContents;
    private Map<ChunkCoords, Integer> offsetTable;
    private boolean modified = false;

    public static String getFileName(ICustomLightSource[] region) {
        final int rx = region[0].getPosition().getRegionX();
        final int rz = region[0].getPosition().getRegionZ();

        if (!allLightSourcesInRegion(rx, rz, region)) {
            throw new IllegalArgumentException("Not all light sources are in the same region!");
        }

        return String.format("r%d.%d.vldb", rx, rz);
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

    public boolean containsChunk(int cx, int cz) {
        return containsChunk(new ChunkCoords(cx, cz));
    }

    public boolean containsChunk(ChunkCoords chunkCoords) {
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

        final ChunkCoords chunkCoords = new ChunkCoords(cx, cz);

        if (containsChunk(chunkCoords)) {
            throw new IllegalStateException("Chunk already in this file!");
        }

        synchronized (lock) {
            Map<ChunkCoords, Integer> newOffsetTable = new HashMap<>();

            for (ChunkCoords key : offsetTable.keySet()) {
                newOffsetTable.put(key, offsetTable.get(key) + OFFSET_TABLE_ENTRY_SIZE);
            }

            newOffsetTable.put(chunkCoords, fileContents.length + OFFSET_TABLE_ENTRY_SIZE);

            Pair<ByteArrayOutputStream, VLDBOutputStream> out = outToMemory();

            out.item2.writeChunk(chunkCoords, chunk);
            out.item2.close();

            final int oldHeaderSize = HEADER_SIZE_WITHOUT_OFFSET_TABLE
                    + offsetTable.keySet().size() * OFFSET_TABLE_ENTRY_SIZE;

            final byte[] append = out.item1.toByteArray();
            Pair<ByteArrayOutputStream, VLDBOutputStream> newFileOut = outToMemory(fileContents.length + OFFSET_TABLE_ENTRY_SIZE + append.length);

            newFileOut.item2.writeHeader(regionX, regionZ, newOffsetTable);
            newFileOut.item2.write(fileContents, oldHeaderSize, fileContents.length - oldHeaderSize);
            newFileOut.item2.write(append);

            newFileOut.item2.close();

            this.offsetTable = newOffsetTable;
            this.fileContents = newFileOut.item1.toByteArray();
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
}
