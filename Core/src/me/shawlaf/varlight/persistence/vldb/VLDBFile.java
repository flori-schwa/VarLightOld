package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;

import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class VLDBFile<L extends ICustomLightSource> {

    private final Object lock = new Object();

    public final File file;

    private final int regionX, regionZ;
    private byte[] fileContents;
    private Map<ChunkCoords, Integer> header;

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

    public VLDBFile(File file) throws IOException {
        this.file = file;

        synchronized (lock) {
            readFully();

            VLDBInputStream headerReader = in();

            this.header = headerReader.readHeader(
                    this.regionX = headerReader.readInt32(),
                    this.regionZ = headerReader.readInt32()
            );

            headerReader.close();
        }
    }

    public L[] readChunk(int chunkX, int chunkZ) throws IOException {
        return readChunk(new ChunkCoords(chunkX, chunkZ));
    }

    public L[] readChunk(ChunkCoords chunkCoords) throws IOException {

        if (chunkCoords.getRegionX() != regionX || chunkCoords.getRegionZ() != regionZ) {
            throw new IllegalArgumentException(String.format("%s not in region %d %d", chunkCoords.toString(), regionX, regionZ));
        }

        if (!header.containsKey(chunkCoords)) {
            return createArray(0);
        }

        synchronized (lock) {
            try (VLDBInputStream in = in(header.get(chunkCoords))) {
                return in.readChunk(regionX, regionZ, this::createArray, this::createInstance).item2;
            }
        }
    }

    public void write(ICustomLightSource[] region) throws IOException {
        synchronized (lock) {
            VLDBOutputStream outputStream = out();

            outputStream.write(region);

            outputStream.close();

            readFully();

            rereadHeader();
        }
    }

    private VLDBInputStream in(int offset) {
        return new VLDBInputStream(new ByteArrayInputStream(fileContents, offset, fileContents.length - offset));
    }

    private VLDBInputStream in() {
        return new VLDBInputStream(new ByteArrayInputStream(fileContents));
    }

    private VLDBOutputStream out() throws IOException {
        return new VLDBOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
    }

    private void readFully() throws IOException {
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
            throw new RuntimeException(String.format("Region information in header changed? (was: %d %d, is: %d %d)", regionX, regionZ, readRx, readRz));
        }

        this.header = headerReader.readHeader(this.regionX, this.regionZ);

        headerReader.close();
    }

    protected abstract L[] createArray(int size);

    protected abstract L createInstance(IntPosition position, int lightLevel, boolean migrated, String material);
}
