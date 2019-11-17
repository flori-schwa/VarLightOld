package me.shawlaf.varlight.test.util;

import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.persistence.vldb.VLDBInputStream;
import me.shawlaf.varlight.persistence.vldb.VLDBOutputStream;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestVLDB {

    @Test
    public void testVLDB() {
        BasicCustomLightSource[][] testData = new BasicCustomLightSource[][]{
                {
                        new BasicCustomLightSource(new IntPosition(0, 0, 0), 15, true, "STONE"),
                        new BasicCustomLightSource(new IntPosition(1, 1, 1), 15, true, "DIRT"),
                        new BasicCustomLightSource(new IntPosition(2, 2, 2), 15, true, "GRAVEL"),
                        new BasicCustomLightSource(new IntPosition(3, 3, 3), 15, true, "SAND"),
                        new BasicCustomLightSource(new IntPosition(4, 4, 4), 15, true, "BEDROCK")
                },
                {
                        new BasicCustomLightSource(new IntPosition(-1, 0, -1), 15, true, "STONE"),
                        new BasicCustomLightSource(new IntPosition(-2, 1, -2), 15, true, "DIRT"),
                        new BasicCustomLightSource(new IntPosition(-3, 2, -3), 15, true, "GRAVEL"),
                        new BasicCustomLightSource(new IntPosition(-4, 3, -4), 15, true, "SAND"),
                        new BasicCustomLightSource(new IntPosition(-5, 4, -5), 15, true, "BEDROCK")
                },
                {
                        new BasicCustomLightSource(new IntPosition(0, 0, -1), 15, true, "STONE"),
                        new BasicCustomLightSource(new IntPosition(1, 1, -2), 15, true, "DIRT"),
                        new BasicCustomLightSource(new IntPosition(2, 2, -3), 15, true, "GRAVEL"),
                        new BasicCustomLightSource(new IntPosition(3, 3, -4), 15, true, "SAND"),
                        new BasicCustomLightSource(new IntPosition(4, 4, -5), 15, true, "BEDROCK")
                },
                {
                        new BasicCustomLightSource(new IntPosition(512, 0, -512), 15, true, "STONE"),
                        new BasicCustomLightSource(new IntPosition(513, 1, -511), 15, true, "DIRT"),
                        new BasicCustomLightSource(new IntPosition(514, 2, -510), 15, true, "GRAVEL"),
                        new BasicCustomLightSource(new IntPosition(515, 3, -509), 15, true, "SAND"),
                        new BasicCustomLightSource(new IntPosition(516, 4, -508), 15, true, "BEDROCK")
                },
                {
                        new BasicCustomLightSource(new IntPosition(-512, 0, 512), 15, true, "STONE"),
                        new BasicCustomLightSource(new IntPosition(-511, 1, 513), 15, true, "DIRT"),
                        new BasicCustomLightSource(new IntPosition(-510, 2, 514), 15, true, "GRAVEL"),
                        new BasicCustomLightSource(new IntPosition(-509, 3, 515), 15, true, "SAND"),
                        new BasicCustomLightSource(new IntPosition(-508, 4, 516), 15, true, "BEDROCK")
                }
        };

        class VLDBTest {
            private BasicCustomLightSource[] lightSources;
            private boolean zipped;
            private byte[] buffer;

            public VLDBTest(BasicCustomLightSource[] data) {
                this.lightSources = data;
                Arrays.sort(lightSources, Comparator.comparing(BasicCustomLightSource::getPosition));
            }

            public void doTest() throws IOException {
                zipped = false;
                BasicCustomLightSource[] read;

                writeToBuffer();
                read = readFromBuffer();
                assertArrayEquals(lightSources, read);

                zipped = true;

                writeToBuffer();
                read = readFromBuffer();
                assertArrayEquals(lightSources, read);
            }

            private void writeToBuffer() throws IOException {
                ByteArrayOutputStream bais = new ByteArrayOutputStream();
                VLDBOutputStream out = new VLDBOutputStream(zipped ? new GZIPOutputStream(bais) : bais);

                out.write(lightSources);
                out.close();

                buffer = bais.toByteArray();
            }

            private BasicCustomLightSource[] readFromBuffer() throws IOException {
                VLDBInputStream in = new VLDBInputStream(
                        zipped ? new GZIPInputStream(new ByteArrayInputStream(buffer)) : new ByteArrayInputStream(buffer)
                );

                BasicCustomLightSource[] read = in.readAll(BasicCustomLightSource[]::new,
                        BasicCustomLightSource::new);

                Arrays.sort(read, Comparator.comparing(BasicCustomLightSource::getPosition));

                return read;
            }
        }

        try {
            for (BasicCustomLightSource[] data : testData) {
                new VLDBTest(data).doTest();
            }

            assertThrows(IllegalArgumentException.class, () -> new VLDBTest(new BasicCustomLightSource[0]).doTest());
        } catch (IOException e) {
            fail("Failed to write or read from buffer", e);
        }
    }

    @Test
    public void testUInt24OutOfRange() {
        try (VLDBOutputStream outputStream = new VLDBOutputStream(new ByteArrayOutputStream())) {
            assertThrows(IllegalArgumentException.class,
                    () -> outputStream.writeUInt24((1 << 24)));
            assertThrows(IllegalArgumentException.class,
                    () -> outputStream.writeUInt24(-1));
        } catch (IOException e) {
            fail("Something messed up", e);
        }
    }

    @Test
    public void testNotAllLightSourcesInSameRegion() {
        try (VLDBOutputStream outputStream = new VLDBOutputStream(new ByteArrayOutputStream())) {

            assertThrows(IllegalArgumentException.class,
                    () -> outputStream.write(new BasicCustomLightSource[]{
                            new BasicCustomLightSource(IntPosition.ORIGIN, 15, true, "STONE"),
                            new BasicCustomLightSource(new IntPosition(1000, 0, 1000), 15, true, "STONE"),
                    })
            );
        } catch (IOException e) {
            fail("Something messed up", e);
        }
    }

    @Test
    public void testHeader() {
        BasicCustomLightSource[] testData = new BasicCustomLightSource[]{
                new BasicCustomLightSource(IntPosition.ORIGIN, 15, true, "STONE"), // l: 2 + 1 + 2 + 5 = 10
                new BasicCustomLightSource(new IntPosition(0, 0, 16), 15, true, "STONE"), // l: 2 + 1 + 2 + 5 = 10
        };

        // Header size: 4 + 4 + 2 + 2 * (2 + 4) = 22

        byte[] buffer = new byte[0];

        // region not zipped

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); VLDBOutputStream out = new VLDBOutputStream(baos)) {
            out.write(testData);
            out.close();

            buffer = baos.toByteArray();
        } catch (IOException e) {
            fail("Something went wrong", e);
        }

        try (VLDBInputStream in = new VLDBInputStream(new ByteArrayInputStream(buffer))) {
            int regionX = in.readInt32();
            int regionZ = in.readInt32();

            assertEquals(0, regionX);
            assertEquals(0, regionZ);

            Map<ChunkCoords, Integer> header = in.readHeader(regionX, regionZ);

            assertEquals(22, header.get(new ChunkCoords(0, 0)));
            assertEquals(22 + (2 + 3 + 10), header.get(new ChunkCoords(0, 1)));

        } catch (IOException e) {
            fail("Something went wrong", e);
        }

        // endregion

        // region zipped

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); VLDBOutputStream out = new VLDBOutputStream(new GZIPOutputStream(baos))) {
            out.write(testData);
            out.close();

            buffer = baos.toByteArray();
        } catch (IOException e) {
            fail("Something went wrong", e);
        }

        try (VLDBInputStream in = new VLDBInputStream(new GZIPInputStream(new ByteArrayInputStream(buffer)))) {
            int regionX = in.readInt32();
            int regionZ = in.readInt32();

            assertEquals(0, regionX);
            assertEquals(0, regionZ);

            Map<ChunkCoords, Integer> header = in.readHeader(regionX, regionZ);

            assertEquals(22, header.get(new ChunkCoords(0, 0)));
            assertEquals(22 + (2 + 3 + 10), header.get(new ChunkCoords(0, 1)));

        } catch (IOException e) {
            fail("Something went wrong", e);
        }

        // endregion

    }

    @Test
    public void testVLDBFile(@TempDir File testDir) {
        BasicCustomLightSource[] testData = new BasicCustomLightSource[]{
                new BasicCustomLightSource(IntPosition.ORIGIN, 15, true, "STONE"), // Chunk 0,0
                new BasicCustomLightSource(new IntPosition(16, 0, 0), 15, true, "STONE"), // Chunk 1,0
                new BasicCustomLightSource(new IntPosition(0, 0, 16), 15, true, "STONE"), // Chunk 0,1
        };

        try {

            File file = new File(testDir, VLDBFile.getFileName(testData));

            try (VLDBOutputStream out = new VLDBOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
                out.write(testData);
            }

            VLDBFile<BasicCustomLightSource> vldbFile = new VLDBFile<BasicCustomLightSource>(file) {
                @Override
                protected BasicCustomLightSource[] createArray(int size) {
                    return new BasicCustomLightSource[size];
                }

                @Override
                protected BasicCustomLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return new BasicCustomLightSource(position, lightLevel, migrated, material);
                }
            };

            BasicCustomLightSource[] readChunk = vldbFile.readChunk(0, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[0], readChunk[0]);

            readChunk = vldbFile.readChunk(1, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[1], readChunk[0]);

            readChunk = vldbFile.readChunk(0, 1);

            assertEquals(1, readChunk.length);
            assertEquals(testData[2], readChunk[0]);

            assertArrayEquals(new BasicCustomLightSource[0], vldbFile.readChunk(2, 2));
            assertThrows(IllegalArgumentException.class, () -> vldbFile.readChunk(-1, -1));

            BasicCustomLightSource[] copy = new BasicCustomLightSource[2];
            System.arraycopy(testData, 0, copy, 0, 2); // Exclude chunk 0,1 from test data

            vldbFile.write(copy);

            readChunk = vldbFile.readChunk(0, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[0], readChunk[0]);

            readChunk = vldbFile.readChunk(1, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[1], readChunk[0]);

            assertArrayEquals(new BasicCustomLightSource[0], vldbFile.readChunk(0, 1));

            assertTrue(vldbFile.file.delete());
        } catch (IOException e) {
            fail("Something went wrong", e);
        }
    }

}
