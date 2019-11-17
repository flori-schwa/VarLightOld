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
        // region Test Data

        BasicCustomLightSource[] testData = new BasicCustomLightSource[]{
                new BasicCustomLightSource(IntPosition.ORIGIN, 15, true, "STONE"), // Chunk 0,0
                new BasicCustomLightSource(new IntPosition(16, 0, 0), 15, true, "STONE"), // Chunk 1,0
                new BasicCustomLightSource(new IntPosition(0, 0, 16), 15, true, "STONE") // Chunk 0,1
        };

        BasicCustomLightSource[] chunk22 = new BasicCustomLightSource[]{
                new BasicCustomLightSource(new IntPosition(32, 0, 32), 15, true, "STONE") // Chunk 2,2
        };

        BasicCustomLightSource[] edited01DifferentLength = new BasicCustomLightSource[]{
                new BasicCustomLightSource(new IntPosition(0, 0, 16), 15, true, "STONE"), // Chunk 0,1
                new BasicCustomLightSource(new IntPosition(0, 5, 16), 15, true, "STONE") // Chunk 0,1
        };

        BasicCustomLightSource[] edited01SameLength = new BasicCustomLightSource[]{
                new BasicCustomLightSource(new IntPosition(0, 0, 16), 15, true, "STONE"), // Chunk 0,1
                new BasicCustomLightSource(new IntPosition(0, 10, 16), 15, true, "STONE") // Chunk 0,1
        };

        // endregion

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

            // region without 2,2

            assertTrue(vldbFile.hasChunkData(0, 0));
            assertTrue(vldbFile.hasChunkData(1, 0));
            assertTrue(vldbFile.hasChunkData(0, 1));
            assertFalse(vldbFile.hasChunkData(2, 2));

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

            // endregion

            // region with 2,2 (testing insertChunk)

            vldbFile.insertChunk(chunk22);

            assertTrue(vldbFile.hasChunkData(0, 0));
            assertTrue(vldbFile.hasChunkData(1, 0));
            assertTrue(vldbFile.hasChunkData(0, 1));
            assertTrue(vldbFile.hasChunkData(2, 2));

            readChunk = vldbFile.readChunk(0, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[0], readChunk[0]);

            readChunk = vldbFile.readChunk(1, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[1], readChunk[0]);

            readChunk = vldbFile.readChunk(0, 1);

            assertEquals(1, readChunk.length);
            assertEquals(testData[2], readChunk[0]);

            readChunk = vldbFile.readChunk(2, 2);

            assertEquals(1, readChunk.length);
            assertEquals(chunk22[0], readChunk[0]);

            assertThrows(IllegalArgumentException.class, () -> vldbFile.readChunk(-1, -1));

            // endregion

            // region without 1,0 (testing removeChunk)

            vldbFile.removeChunk(new ChunkCoords(1, 0));

            assertTrue(vldbFile.hasChunkData(0, 0));
            assertFalse(vldbFile.hasChunkData(1, 0));
            assertTrue(vldbFile.hasChunkData(0, 1));
            assertTrue(vldbFile.hasChunkData(2, 2));

            readChunk = vldbFile.readChunk(0, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[0], readChunk[0]);

            readChunk = vldbFile.readChunk(0, 1);

            assertEquals(1, readChunk.length);
            assertEquals(testData[2], readChunk[0]);

            readChunk = vldbFile.readChunk(2, 2);

            assertEquals(1, readChunk.length);
            assertEquals(chunk22[0], readChunk[0]);

            assertArrayEquals(new BasicCustomLightSource[0], vldbFile.readChunk(1, 0));
            assertThrows(IllegalArgumentException.class, () -> vldbFile.readChunk(-1, -1));

            // endregion

            // region Editing 0,1 with different length

            vldbFile.editChunk(new ChunkCoords(0, 1), edited01DifferentLength);

            assertTrue(vldbFile.hasChunkData(0, 0));
            assertFalse(vldbFile.hasChunkData(1, 0));
            assertTrue(vldbFile.hasChunkData(0, 1));
            assertTrue(vldbFile.hasChunkData(2, 2));

            readChunk = vldbFile.readChunk(0, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[0], readChunk[0]);

            readChunk = vldbFile.readChunk(0, 1);

            assertEquals(2, readChunk.length);
            assertArrayEquals(edited01DifferentLength, readChunk);

            readChunk = vldbFile.readChunk(2, 2);

            assertEquals(1, readChunk.length);
            assertEquals(chunk22[0], readChunk[0]);

            assertArrayEquals(new BasicCustomLightSource[0], vldbFile.readChunk(1, 0));
            assertThrows(IllegalArgumentException.class, () -> vldbFile.readChunk(-1, -1));

            // endregion

            // region Editing 0,1 with same length

            vldbFile.editChunk(new ChunkCoords(0, 1), edited01SameLength);

            assertTrue(vldbFile.hasChunkData(0, 0));
            assertFalse(vldbFile.hasChunkData(1, 0));
            assertTrue(vldbFile.hasChunkData(0, 1));
            assertTrue(vldbFile.hasChunkData(2, 2));

            readChunk = vldbFile.readChunk(0, 0);

            assertEquals(1, readChunk.length);
            assertEquals(testData[0], readChunk[0]);

            readChunk = vldbFile.readChunk(0, 1);

            assertEquals(2, readChunk.length);
            assertArrayEquals(edited01SameLength, readChunk);

            readChunk = vldbFile.readChunk(2, 2);

            assertEquals(1, readChunk.length);
            assertEquals(chunk22[0], readChunk[0]);

            assertArrayEquals(new BasicCustomLightSource[0], vldbFile.readChunk(1, 0));
            assertThrows(IllegalArgumentException.class, () -> vldbFile.readChunk(-1, -1));

            // endregion

            // region testing save

            vldbFile.save();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(vldbFile.file))) {
                byte[] buffer = new byte[1024];
                int read = 0;

                while ((read = in.read(buffer)) > 0) {
                    baos.write(buffer, 0, read);
                }
            }

            assertArrayEquals(vldbFile.fileContents, baos.toByteArray());
            // endregion

            // region testing bad input for insertChunk

            assertThrows(IllegalArgumentException.class,
                    () -> vldbFile.insertChunk(new BasicCustomLightSource[0])
            );

            assertThrows(IllegalArgumentException.class,
                    () -> vldbFile.insertChunk(new BasicCustomLightSource[]{
                            new BasicCustomLightSource(new IntPosition(5 * 16 + 1, 0, 5 * 16 + 1), 15, true, "STONE"),
                            new BasicCustomLightSource(new IntPosition(6 * 16 + 1, 0, 5 * 16 + 1), 15, true, "STONE"),
                    })
            );

            assertThrows(IllegalArgumentException.class,
                    () -> vldbFile.insertChunk(new BasicCustomLightSource[]{
                            new BasicCustomLightSource(new IntPosition(-1, 0, -1), 15, true, "STONE")
                    })
            );

            assertThrows(IllegalStateException.class,
                    () -> vldbFile.insertChunk(new BasicCustomLightSource[]{
                            new BasicCustomLightSource(IntPosition.ORIGIN, 15, true, "STONE")
                    })
            );

            // endregion

            // region testing bad input for editChunk

            assertThrows(IllegalArgumentException.class, () -> vldbFile.editChunk(ChunkCoords.ORIGIN, new BasicCustomLightSource[0]));

            assertThrows(IllegalArgumentException.class,
                    () -> vldbFile.editChunk(new ChunkCoords(-1, -1), new BasicCustomLightSource[]{
                            new BasicCustomLightSource(new IntPosition(-1, 0, -1), 15, true, "STONE")
                    })
            );

            assertThrows(IllegalArgumentException.class,
                    () -> vldbFile.editChunk(ChunkCoords.ORIGIN, new BasicCustomLightSource[]{
                            new BasicCustomLightSource(new IntPosition(1000, 0, 0), 15, true, "STONE")
                    })
            );

            assertThrows(IllegalArgumentException.class,
                    () -> vldbFile.editChunk(new ChunkCoords(3, 3), new BasicCustomLightSource[]{
                            new BasicCustomLightSource(new IntPosition(3 * 16 + 1, 0, 3 * 16 + 1), 15, true, "STONE")
                    })
            );

            // endregion

            // region testing bad input for removeChunk

            assertThrows(IllegalArgumentException.class,
                    () -> vldbFile.removeChunk(new ChunkCoords(-1, -1))
            );

            assertThrows(IllegalStateException.class,
                    () -> vldbFile.removeChunk(new ChunkCoords(16, 16))
            );

            // endregion

            assertTrue(vldbFile.file.delete());
        } catch (IOException e) {
            fail("Something went wrong", e);
        }
    }
}
