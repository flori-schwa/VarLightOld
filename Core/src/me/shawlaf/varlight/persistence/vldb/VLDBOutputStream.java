package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    .VLDB Format:

    Light Source:

    [int16  ]       Relative coords in chunk (x: 0xF000, y: 0x0FF0, z: 0x000F)
    [byte   ]       Light source data:
                        MSB: [Nibble]   Custom light value (0 - F)
                        LSB: [Bool]     migrated
    [ASCII  ] Material

    ASCII encoding:

    [int16  ]           Length
    [Byte[Length]]      ASCII Bytes

    Chunk:

    [int16  ]       Relative coords of chunk in region (x: 0xFF00, z: 0x00FF)
    [uint24 ]       Amount of light sources in chunk, length of the next array
    [LightSource[]] Light Sources in chunk

    Header Format:

    [int32  ]       region X
    [int32  ]       region Z
    [int16  ]       total amount of chunks with custom light sources
    [Array  ]       Offset table:
                [int16  ]       Relative coords of chunk in region (x: 0xFF00, z: 0x00FF)
                [int32  ]       File offset for this chunk's Data
     */

public class VLDBOutputStream implements Flushable, Closeable, AutoCloseable {

    protected final DataOutputStream baseStream;

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
                ((lightSource.getPosition().x & 0xF) << 12) |       // Encoded X
                        ((lightSource.getPosition().y) << 4) |         // Encoded Y
                        (lightSource.getPosition().z & 0xF));          // Encoded Z
        baseStream.writeByte(((lightSource.getCustomLuminance() & 0xF) << 4) | (lightSource.isMigrated() ? 1 : 0));
        writeASCII(lightSource.getType());
    }

    public void writeChunk(int chunkX, int chunkZ, ICustomLightSource[] lightSources) throws IOException {
        baseStream.writeShort((((chunkX % 32) + 32) % 32) << 8 | ((chunkZ % 32) + 32) % 32);
        writeUInt24(lightSources.length);

        for (ICustomLightSource lightSource : lightSources) {
            writeLightSource(lightSource);
        }
    }

    public void write(ICustomLightSource[] region) throws IOException {
        if (region.length == 0) {
            throw new IllegalArgumentException("Amount of light sources must be > 0");
        }

        final int rx = region[0].getPosition().getRegionX();
        final int rz = region[0].getPosition().getRegionZ();

        if (!VLDBFile.allLightSourcesInRegion(rx, rz, region)) {
            throw new IllegalArgumentException("Not all light sources are in the same region!");
        }

        final Map<ChunkCoords, List<ICustomLightSource>> chunkMap = new HashMap<>();
        final Map<ChunkCoords, Integer> offsetTable = new HashMap<>();

        for (int i = 0; i < region.length; i++) {
            ChunkCoords chunkCoords = region[i].getPosition().toChunkCoords();

            if (!chunkMap.containsKey(chunkCoords)) {
                chunkMap.put(chunkCoords, new ArrayList<>());
            }

            chunkMap.get(chunkCoords).add(region[i]);
        }

        final ChunkCoords[] chunks = chunkMap.keySet().toArray(new ChunkCoords[0]);

        final int headerSize = VLDBUtil.SIZEOF_HEADER_WITHOUT_OFFSET_TABLE + chunks.length * VLDBUtil.SIZEOF_OFFSET_TABLE_ENTRY;

        final ByteArrayOutputStream fileBodyBuffer = new ByteArrayOutputStream();
        final VLDBOutputStream bodyOutputStream = new VLDBOutputStream(fileBodyBuffer);

        for (int i = 0; i < chunks.length; i++) {
            ChunkCoords chunkCoords = chunks[i];

            offsetTable.put(chunkCoords, headerSize + fileBodyBuffer.size());
            bodyOutputStream.writeChunk(chunkCoords.x, chunkCoords.z, chunkMap.get(chunkCoords).toArray(new ICustomLightSource[0]));
        }

        writeHeader(rx, rz, offsetTable);
        write(fileBodyBuffer.toByteArray());
    }

    public void writeHeader(int regionX, int regionZ, Map<ChunkCoords, Integer> offsetTable) throws IOException {
        writeInt32(VLDBInputStream.VLDB_MAGIC);
        writeInt32(regionX);
        writeInt32(regionZ);
        writeInt16(offsetTable.keySet().size());

        for (Map.Entry<ChunkCoords, Integer> entry : offsetTable.entrySet()) {
            writeInt16((entry.getKey().getRegionRelativeX()) << 8 | entry.getKey().getRegionRelativeZ());
            writeInt32(entry.getValue());
        }
    }

    public void writeASCII(String data) throws IOException {
        byte[] asciiData = data.getBytes(StandardCharsets.US_ASCII);

        writeInt16(asciiData.length);
        write(asciiData);
    }

    public void writeByte(int b) throws IOException {
        baseStream.writeByte(b);
    }

    public void writeInt16(int i16) throws IOException {
        baseStream.writeShort(i16);
    }

    public void writeUInt24(int ui24) throws IOException {
        if (ui24 < 0 || ui24 > ((1 << 24) - 1)) {
            throw new IllegalArgumentException("UInt24 out of range!");
        }

        writeByte((ui24 >>> 16) & 0xFF);
        writeByte((ui24 >>> 8) & 0xFF);
        writeByte(ui24 & 0xFF);
    }

    public void writeInt32(int i32) throws IOException {
        baseStream.writeInt(i32);
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int off, int len) throws IOException {
        baseStream.write(data, off, len);
    }
}
