package me.shawlaf.varlight.test.util;

import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.vldb.VLDBInputStream;
import me.shawlaf.varlight.persistence.vldb.VLDBOutputStream;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TestVLDB {

    @Test
    public void testVLDB() {
        BasicCustomLightSource[][] testData = new BasicCustomLightSource[][]{
                {
                        new BasicCustomLightSource(new IntPosition(0, 0, 0), "STONE", 15, true),
                        new BasicCustomLightSource(new IntPosition(1, 1, 1), "DIRT", 15, true),
                        new BasicCustomLightSource(new IntPosition(2, 2, 2), "GRAVEL", 15, true),
                        new BasicCustomLightSource(new IntPosition(3, 3, 3), "SAND", 15, true),
                        new BasicCustomLightSource(new IntPosition(4, 4, 4), "BEDROCK", 15, true)
                },
                {
                        new BasicCustomLightSource(new IntPosition(-1, 0, -1), "STONE", 15, true),
                        new BasicCustomLightSource(new IntPosition(-2, 1, -2), "DIRT", 15, true),
                        new BasicCustomLightSource(new IntPosition(-3, 2, -3), "GRAVEL", 15, true),
                        new BasicCustomLightSource(new IntPosition(-4, 3, -4), "SAND", 15, true),
                        new BasicCustomLightSource(new IntPosition(-5, 4, -5), "BEDROCK", 15, true)
                },
                {
                        new BasicCustomLightSource(new IntPosition(0, 0, -1), "STONE", 15, true),
                        new BasicCustomLightSource(new IntPosition(1, 1, -2), "DIRT", 15, true),
                        new BasicCustomLightSource(new IntPosition(2, 2, -3), "GRAVEL", 15, true),
                        new BasicCustomLightSource(new IntPosition(3, 3, -4), "SAND", 15, true),
                        new BasicCustomLightSource(new IntPosition(4, 4, -5), "BEDROCK", 15, true)
                },
                {
                        new BasicCustomLightSource(new IntPosition(512, 0, -512), "STONE", 15, true),
                        new BasicCustomLightSource(new IntPosition(513, 1, -511), "DIRT", 15, true),
                        new BasicCustomLightSource(new IntPosition(514, 2, -510), "GRAVEL", 15, true),
                        new BasicCustomLightSource(new IntPosition(515, 3, -509), "SAND", 15, true),
                        new BasicCustomLightSource(new IntPosition(516, 4, -508), "BEDROCK", 15, true)
                },
                {
                        new BasicCustomLightSource(new IntPosition(-512, 0, 512), "STONE", 15, true),
                        new BasicCustomLightSource(new IntPosition(-511, 1, 513), "DIRT", 15, true),
                        new BasicCustomLightSource(new IntPosition(-510, 2, 514), "GRAVEL", 15, true),
                        new BasicCustomLightSource(new IntPosition(-509, 3, 515), "SAND", 15, true),
                        new BasicCustomLightSource(new IntPosition(-508, 4, 516), "BEDROCK", 15, true)
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
                Assertions.assertArrayEquals(lightSources, read);

                zipped = true;

                writeToBuffer();
                read = readFromBuffer();
                Assertions.assertArrayEquals(lightSources, read);
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
                        (position, lightLevel, migrated, material) -> new BasicCustomLightSource(position, material, lightLevel, migrated));

                Arrays.sort(read, Comparator.comparing(BasicCustomLightSource::getPosition));

                return read;
            }
        }

        try {
            for (BasicCustomLightSource[] data : testData) {
                new VLDBTest(data).doTest();
            }

            Assertions.assertThrows(IllegalArgumentException.class, () -> new VLDBTest(new BasicCustomLightSource[0]).doTest());
        } catch (IOException e) {
            Assertions.fail("Failed to write or read from buffer", e);
        }
    }

    @Test
    public void testUInt24OutOfRange() {
        try (VLDBOutputStream outputStream = new VLDBOutputStream(new ByteArrayOutputStream())) {
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> outputStream.writeUInt24((1 << 24)));
        } catch (IOException e) {
            Assertions.fail("Something messed up", e);
        }
    }

    @Test
    public void testNotAllLightSourcesInSameRegion() {
        try (VLDBOutputStream outputStream = new VLDBOutputStream(new ByteArrayOutputStream())) {

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> outputStream.write(new BasicCustomLightSource[]{
                            new BasicCustomLightSource(IntPosition.ORIGIN, "STONE", 15, true),
                            new BasicCustomLightSource(new IntPosition(1000, 0, 1000), "STONE", 15, true),
                    })
            );
        } catch (IOException e) {
            Assertions.fail("Something messed up", e);
        }
    }

    @Test
    public void testHeader() {
        BasicCustomLightSource[] testData = new BasicCustomLightSource[]{
                new BasicCustomLightSource(IntPosition.ORIGIN, "STONE", 15, true), // l: 2 + 1 + 2 + 5 = 10
                new BasicCustomLightSource(new IntPosition(0, 0, 16), "STONE", 15, true), // l: 2 + 1 + 2 + 5 = 10
        };

        // Header size: 4 + 4 + 2 + 2 * (2 + 4) = 22

        byte[] buffer = new byte[0];

        // region not zipped

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); VLDBOutputStream out = new VLDBOutputStream(baos)) {
            out.write(testData);
            out.close();

            buffer = baos.toByteArray();
        } catch (IOException e) {
            Assertions.fail("Something went wrong", e);
        }

        try (VLDBInputStream in = new VLDBInputStream(new ByteArrayInputStream(buffer))) {
            int regionX = in.readInt32();
            int regionZ = in.readInt32();

            Assertions.assertEquals(0, regionX);
            Assertions.assertEquals(0, regionZ);

            Map<ChunkCoords, Integer> header = in.readHeader(regionX, regionZ);

            Assertions.assertEquals(22, header.get(new ChunkCoords(0, 0)));
            Assertions.assertEquals(22 + (2 + 3 + 10), header.get(new ChunkCoords(0, 1)));

        } catch (IOException e) {
            Assertions.fail("Something went wrong", e);
        }

        // endregion

        // region zipped

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); VLDBOutputStream out = new VLDBOutputStream(new GZIPOutputStream(baos))) {
            out.write(testData);
            out.close();

            buffer = baos.toByteArray();
        } catch (IOException e) {
            Assertions.fail("Something went wrong", e);
        }

        try (VLDBInputStream in = new VLDBInputStream(new GZIPInputStream(new ByteArrayInputStream(buffer)))) {
            int regionX = in.readInt32();
            int regionZ = in.readInt32();

            Assertions.assertEquals(0, regionX);
            Assertions.assertEquals(0, regionZ);

            Map<ChunkCoords, Integer> header = in.readHeader(regionX, regionZ);

            Assertions.assertEquals(22, header.get(new ChunkCoords(0, 0)));
            Assertions.assertEquals(22 + (2 + 3 + 10), header.get(new ChunkCoords(0, 1)));

        } catch (IOException e) {
            Assertions.fail("Something went wrong", e);
        }

        // endregion

    }

}
