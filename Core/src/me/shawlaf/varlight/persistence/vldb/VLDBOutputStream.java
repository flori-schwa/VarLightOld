package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.ICustomLightSource;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

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

    public static void main(String[] args) throws IOException {
        final boolean zipped = false;
        final int regionX = 0, regionZ = 0;

        final Random random = new Random();

        File testFile = new File("C:\\temp\\vldb_test.vldb");


        if (testFile.exists()) {
            testFile.delete();
        }

        testFile.createNewFile();

        ICustomLightSource[] toWrite = new ICustomLightSource[32 * 32 * 16];
//        ICustomLightSource[] toWrite = new ICustomLightSource[32 * 32 * 16 * 16 * 256];
        int count = 0;

        for (int rcx = 0; rcx < 32; rcx++) {
            for (int rcz = 0; rcz < 32; rcz++) {
                final int cx = rcx + (regionX * 32);
                final int cz = rcz + (regionZ * 32);

                for (int cy = 0; cy < 16; cy++) {
                    toWrite[count++] = new BasicCustomLightSource(
                            new IntPosition(
                                    cx * 16 + (random.nextInt(16)),
                                    cy * 16 + (random.nextInt(16)),
                                    cz * 16 + (random.nextInt(16))
                            ),
                            random.nextInt(16), random.nextBoolean(), "STONE"
                    );
                }

//                for (int y = 0; y < 256; y++) {
//                    for (int z = 0; z < 16; z++) {
//                        for (int x = 0; x < 16; x++) {
//                                toWrite[count++] = new BasicCustomLightSource(
//                                        new IntPosition(cx * 16 + x, y, cz * 16 + z),
//                                        "STONE",
//                                        random.nextInt(16),
//                                        random.nextBoolean()
//                                );
//                        }
//                    }
//                }
            }
        }

        try {
            VLDBOutputStream outputStream;

            if (zipped) {
                outputStream = new VLDBOutputStream(new GZIPOutputStream(new FileOutputStream(testFile)));
            } else {
                outputStream = new VLDBOutputStream(new FileOutputStream(testFile));
            }

            long start = System.currentTimeMillis();

            outputStream.write(toWrite);
            outputStream.close();

            long total = System.currentTimeMillis() - start;

            System.out.println("Writing took " + total + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }

//        File jsonTest = new File("C:\\temp\\vldb_test.json");
//
//        if (jsonTest.exists()) {
//            jsonTest.delete();
//        }
//
//        jsonTest.createNewFile();
//
//        Gson gson = new Gson();
//
//        new FileWriter(jsonTest).write(gson.toJson(toWrite));
    }

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

    public void writeChunk(ChunkCoords chunkCoords, ICustomLightSource[] lightSources) throws IOException {
        baseStream.writeShort((chunkCoords.getRegionRelativeX()) << 8 | chunkCoords.getRegionRelativeZ());
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
            bodyOutputStream.writeChunk(chunkCoords, chunkMap.get(chunkCoords).toArray(new ICustomLightSource[0]));
        }

        writeHeader(rx, rz, offsetTable);
        write(fileBodyBuffer.toByteArray());
    }

    public void writeHeader(int regionX, int regionZ, Map<ChunkCoords, Integer> offsetTable) throws IOException {
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