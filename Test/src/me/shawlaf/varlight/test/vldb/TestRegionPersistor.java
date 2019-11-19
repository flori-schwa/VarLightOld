package me.shawlaf.varlight.test.vldb;

import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.RegionPersistor;
import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.persistence.vldb.VLDBInputStream;
import me.shawlaf.varlight.util.IntPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestRegionPersistor {

    private static class RegionPersistorBasic extends RegionPersistor<BasicCustomLightSource> {
        public RegionPersistorBasic(@NotNull File vldbRoot, int regionX, int regionZ) throws IOException {
            super(vldbRoot, regionX, regionZ);
        }

        @Override
        protected BasicCustomLightSource[] createArray(int size) {
            return new BasicCustomLightSource[size];
        }

        @Override
        protected BasicCustomLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
            return new BasicCustomLightSource(position, lightLevel, migrated, material);
        }
    }

    private File tempDir;

    @BeforeEach
    public void initTempDir(@TempDir File tempDir) {
        this.tempDir = tempDir;
    }

    @Test
    public void testConstructorFileNotExist() {
        try {

            final int rx = 0, rz = 0;

            RegionPersistorBasic persistorBasic = new RegionPersistorBasic(tempDir, rx, rz);

            assertEquals(rx, persistorBasic.regionX);
            assertEquals(rz, persistorBasic.regionZ);

            persistorBasic.file.save();

            File saveFile = new File(tempDir, String.format(VLDBFile.FILE_NAME_FORMAT, rx, rz));

            assertTrue(saveFile.exists());

            try (FileInputStream fis = new FileInputStream(saveFile)) {
                VLDBInputStream vis = new VLDBInputStream(new GZIPInputStream(fis));

                assertEquals(rx, vis.readInt32());
                assertEquals(rz, vis.readInt32());
                assertEquals(0, vis.readInt16());

                assertEquals(0, fis.available());
            }
        } catch (IOException e) {
            fail(e);
        }

    }

}
