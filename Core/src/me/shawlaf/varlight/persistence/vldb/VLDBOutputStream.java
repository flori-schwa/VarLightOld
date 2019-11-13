package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;

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

    NEW .VLDB Format:

    Light Source:

    [int16  ]       Relative coords in chunk (x: 0xF000, y: 0x0FF0, z: 0x000F)
    [byte   ]       Light source data:
                        MSB: [Nibble]   Custom light value (0 - F)
                        LSB: [Bool]     migrated
    [ASCII  ] Material

    Chunk:

    [int16  ]       Relative coords of chunk in region (x: 0xFF00, z: 0x00FF)
    [int24  ]       Amount of light sources in chunk, length of the next array
    [LightSource[]] Light Sources in chunk

    Complete File Format:

    [int16  ]       total amount of chunks with custom light sources
    [Array  ]       Offset table:
                [int16  ]       Relative coords of chunk in region (x: 0xFF00, z: 0x00FF)
                [int32  ]       File offset for this chunk's Data
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

    public void writeLightSource(ICustomLightSource lightSource) throws IOException {
        baseStream.writeShort(
                ((lightSource.getPosition().getX() & 0xF) << 12) |       // Encoded X
                        ((lightSource.getPosition().getY()) << 4) |         // Encoded Y
                        (lightSource.getPosition().getZ() & 0xF));          // Encoded Z
        baseStream.writeByte(((lightSource.getEmittingLight() & 0xF) << 4) | (lightSource.isMigrated() ? 1 : 0));
        writeASCII(lightSource.getType().name());
    }

    public void writeChunk(int chunkX, int chunkZ, ICustomLightSource[] lightSources) throws IOException {
        baseStream.writeShort((chunkX & 0xFF) << 8 | (chunkZ & 0xFF));
        writeInt24(lightSources.length);

        for (ICustomLightSource lightSource : lightSources) {
            writeLightSource(lightSource);
        }
    }

    public void writeInt24(int i24) throws IOException {
        if (i24 < 0 || i24 > ((1 << 24) - 1)) {
            throw new IllegalArgumentException("Int24 out of range!");
        }

        baseStream.writeByte((i24 >>> 16) & 0xFF);
        baseStream.writeByte((i24 >>> 8) & 0xFF);
        baseStream.writeByte(i24 & 0xFF);
    }

    //    public void write(ICustomLightSource[] lightSources) throws IOException {
//        writeInt(lightSources.length);
//
//        for (int i = 0; i < lightSources.length; i++) {
//            writeLightSource(lightSources[i]);
//        }
//    }
//
//    public void writeByte(int b) throws IOException {
//        baseStream.writeByte(b);
//    }
//
//    public void writeInt(int v) throws IOException {
//        baseStream.writeInt(v);
//    }
//
//    public void writePosition(IntPosition position) throws IOException {
//        baseStream.writeLong(position.encode());
//    }
//
    public void writeASCII(String data) throws IOException {
        byte[] asciiData = data.getBytes(StandardCharsets.US_ASCII);

        baseStream.writeShort(asciiData.length);
        baseStream.write(asciiData);
    }
//
//    public void writeLightSource(ICustomLightSource lightSource) throws IOException {
//        writePosition(lightSource.getPosition());
//        writeByte(((lightSource.getEmittingLight() & 0xF) << 4) | (lightSource.isMigrated() ? 1 : 0));
//        writeASCII(lightSource.getType().name());
//    }
}
