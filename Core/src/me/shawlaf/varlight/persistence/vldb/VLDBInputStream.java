package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.Pair;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntFunction;

public class VLDBInputStream implements Closeable {

    protected final DataInputStream baseStream;

    public VLDBInputStream(DataInputStream baseStream) {
        this.baseStream = baseStream;
    }

    public VLDBInputStream(InputStream inputStream) {
        this(new DataInputStream(inputStream));
    }

    @Override
    public void close() throws IOException {
        this.baseStream.close();
    }

    public <L extends ICustomLightSource> L[] readAll(IntFunction<L[]> arrayCreator, ToLightSource<L> toLightSource) throws IOException {
        final int regionX = readInt32();
        final int regionZ = readInt32();

        List<L> lightSources = new ArrayList<>();

        final short amountChunks = readInt16();

        baseStream.skipBytes(amountChunks * (2 + 4)); // Skip header

        for (int i = 0; i < amountChunks; i++) {
            lightSources.addAll(Arrays.asList(readChunk(regionX, regionZ, arrayCreator, toLightSource).item2));
        }

        return lightSources.toArray(arrayCreator.apply(0));
    }

    public <L extends ICustomLightSource> Pair<ChunkCoords, L[]> readChunk(int regionX, int regionZ, IntFunction<L[]> arrayCreator, ToLightSource<L> toLightSource) throws IOException {
        short encodedCoords = readInt16();

        int cx = ((encodedCoords & 0xFF00) >>> 8) + (regionX * 32);
        int cz = (encodedCoords & 0xFF) + (regionZ * 32);

        int amountLightSources = readUInt24();

        L[] lightSources = arrayCreator.apply(amountLightSources);

        for (int j = 0; j < amountLightSources; j++) {
            int coords = readInt16();
            byte data = readByte();
            String material = readASCII();

            IntPosition position = new IntPosition(
                    cx * 16 + ((coords & 0xF000) >>> 12),
                    (coords & 0x0FF0) >>> 4,
                    cz * 16 + (coords & 0xF)
            );

            int lightLevel = (data & 0xF0) >>> 4;
            boolean migrated = (data & 0x0F) != 0;

            lightSources[j] = toLightSource.toLightSource(position, lightLevel, migrated, material);
        }

        return new Pair<>(new ChunkCoords(cx, cz), lightSources);
    }

    public Map<ChunkCoords, Integer> readHeader(int regionX, int regionZ) throws IOException {
        final int amountChunks = readInt16();

        final Map<ChunkCoords, Integer> header = new HashMap<>(amountChunks);

        for (int i = 0; i < amountChunks; i++) {
            int encodedCoords = readInt16();
            int offset = readInt32();

            int cx = ((encodedCoords & 0xFF00) >>> 8) + (regionX * 32);
            int cz = (encodedCoords & 0xFF) + (regionZ * 32);

            header.put(new ChunkCoords(cx, cz), offset);
        }

        return header;
    }

    public byte readByte() throws IOException {
        return baseStream.readByte();
    }

    public short readInt16() throws IOException {
        return baseStream.readShort();
    }

    public int readUInt24() throws IOException {
        return (readByte() << 16) | (readByte() << 8) | (readByte());
    }

    public int readInt32() throws IOException {
        return baseStream.readInt();
    }

    public String readASCII() throws IOException {
        byte[] asciiBuffer = new byte[readInt16()];
        baseStream.read(asciiBuffer);

        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(asciiBuffer)).toString();
    }

    @FunctionalInterface
    public interface ToLightSource<L extends ICustomLightSource> {
        L toLightSource(IntPosition position, int lightLevel, boolean migrated, String material);
    }
}