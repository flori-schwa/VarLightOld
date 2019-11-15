package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Reference2IntMap;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

    public Reference2IntMap<ChunkCoords> readHeader(int regionX, int regionZ) throws IOException {
        final int amountChunks = readInt16();

        final Reference2IntMap<ChunkCoords> header = new Reference2IntArrayMap<>(amountChunks);

        for (int i = 0; i < amountChunks; i++) {
            int encodedCoords = readInt16();
            int offset = readInt32();

            int cx = (encodedCoords >>> 8) + (regionX * 32);
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

    @Deprecated
    public IntPosition readPosition() throws IOException {
        return new IntPosition(baseStream.readLong());
    }

    public String readASCII() throws IOException {
        byte[] asciiBuffer = new byte[readInt16()];
        baseStream.read(asciiBuffer);

        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(asciiBuffer)).toString();
    }
}
