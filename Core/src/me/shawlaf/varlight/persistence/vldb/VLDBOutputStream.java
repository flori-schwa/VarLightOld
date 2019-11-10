package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.IntPosition;

import java.io.*;
import java.nio.charset.StandardCharsets;

/*
    .VLDB File format:

    [int32]     total amount of custom light sources (N)
    [Array(N)]
        [int64] encoded coords of custom light source
        [byte]  Light source data
            MSB: [Nibble]   Custom light value (0 - F)
            LSB: [Bool]     migrated
        [ASCII] Material

    ASCII encoding:

    [int16]         Length
    [Byte(Length)]  ASCII Bytes

     */

public class VLDBOutputStream implements Flushable, Closeable, AutoCloseable {

    private final DataOutputStream baseStream;

    public VLDBOutputStream(DataOutputStream baseStream) {
        this.baseStream = baseStream;
    }

    public VLDBOutputStream(OutputStream outputStream) {
        this(new DataOutputStream(outputStream));
    }

    @Override
    public void close() throws IOException {
        this.baseStream.close();
    }

    @Override
    public void flush() throws IOException {
        this.baseStream.flush();
    }

    public void write(ICustomLightSource[] lightSources) throws IOException {
        writeInt(lightSources.length);

        for (int i = 0; i < lightSources.length; i++) {
            writeLightSource(lightSources[i]);
        }
    }

    public void writeByte(int b) throws IOException {
        baseStream.writeByte(b);
    }

    public void writeInt(int v) throws IOException {
        baseStream.writeInt(v);
    }

    public void writePosition(IntPosition position) throws IOException {
        baseStream.writeLong(position.encode());
    }

    public void writeASCII(String data) throws IOException {
        byte[] asciiData = data.getBytes(StandardCharsets.US_ASCII);

        baseStream.writeShort(asciiData.length);
        baseStream.write(asciiData);
    }

    public void writeLightSource(ICustomLightSource lightSource) throws IOException {
        writePosition(lightSource.getPosition());
        writeByte(((lightSource.getEmittingLight() & 0xF) << 4) | (lightSource.isMigrated() ? 1 : 0));
        writeASCII(lightSource.getType().name());
    }
}
