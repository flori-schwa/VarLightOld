package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Reference2IntMap;

import java.io.*;
import java.util.function.IntFunction;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// Only used for reading
public class VLDBFile {

    @FunctionalInterface
    public interface ToLightSource<L extends ICustomLightSource> {
        L toLightSource(IntPosition position, int lightLevel, boolean migrated, String material);
    }

    private final Object lock = new Object();

    public final File file;

    private final int regionX, regionZ;
    private byte[] fileContents;
    private Reference2IntMap<ChunkCoords> header;
//    private VLDBInputStream in;

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

    public <L extends ICustomLightSource> L[] readChunk(ChunkCoords chunkCoords, IntFunction<L[]> arrayCreator, ToLightSource<L> constructor) throws IOException {
        if (!header.containsKey(chunkCoords)) {
            return arrayCreator.apply(0);
        }

        synchronized (lock) {
            VLDBInputStream in = in(header.getInt(chunkCoords));

            int encodedCoords = in.readInt16();

            int cx = (encodedCoords >>> 8) + (regionX * 32);
            int cz = (encodedCoords & 0xFF) + (regionZ * 32);

            if (cx != chunkCoords.x || cz != chunkCoords.z) {
                throw new RuntimeException(String.format("Expected chunk (%d, %d) but got (%d, %d)", chunkCoords.x, chunkCoords.z, cx, cz));
            }

            L[] lightSources = arrayCreator.apply(in.readUInt24());

            for (int i = 0; i < lightSources.length; i++) {
                int coords = in.readInt16();
                byte data = in.readByte();
                String material = in.readASCII();

                IntPosition position = new IntPosition(
                        cx * 16 + (coords >>> 12),
                        (coords & 0x0FF0) >>> 4,
                        cz * 16 + (coords & 0xF)
                );

                int lightLevel = data >>> 4;
                boolean migrated = (data & 0xF) != 0;

                lightSources[i] = constructor.toLightSource(position, lightLevel, migrated, material);
            }

            in.close();

            return lightSources;
        }
    }

    public void write(ICustomLightSource[] region) throws IOException {
        synchronized (lock) {
            VLDBOutputStream outputStream = out();

            outputStream.write(region);

            outputStream.close();

            readFully();
        }
    }

    private VLDBInputStream in(int offset) throws IOException {
        VLDBInputStream in = in();
        in.baseStream.skipBytes(offset);

        return in;
    }

    private VLDBInputStream in(int offset, int length) {
        byte[] off = new byte[length];
        System.arraycopy(fileContents, offset, off, 0, length);

        return new VLDBInputStream(new ByteArrayInputStream(off));
    }

    private VLDBInputStream in() {
        return new VLDBInputStream(new ByteArrayInputStream(fileContents));
    }

    private VLDBOutputStream out() throws IOException {
        return new VLDBOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
    }

    private void readFully() throws IOException {
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(file));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];

        while (in.available() == 1) {
            out.write(buffer, 0, in.read(buffer));
        }

        this.fileContents = out.toByteArray();

        in.close();
    }
}
