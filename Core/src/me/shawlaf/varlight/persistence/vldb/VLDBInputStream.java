package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.util.IntPosition;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class VLDBInputStream implements Closeable {

    private final DataInputStream baseStream;

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

    public byte readByte() throws IOException {
        return baseStream.readByte();
    }

    public int readInt() throws IOException {
        return baseStream.readInt();
    }

    public IntPosition readPosition() throws IOException {
        return new IntPosition(baseStream.readLong());
    }

    public String readASCII() throws IOException {
        byte[] asciiBuffer = new byte[baseStream.readShort()];
        baseStream.read(asciiBuffer);

        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(asciiBuffer)).toString();
    }
}
