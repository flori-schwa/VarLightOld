package me.shawlaf.varlight.spigot.test.persistence.migrate.data;

import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.RegionPersistor;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.persistence.migrate.data.VLDBToNLSMigration;
import me.shawlaf.varlight.util.IntPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestVLDBToNLS {

    @Mock
    private VarLightPlugin plugin = mock(VarLightPlugin.class);

    @Test
    public void testVLDBToNLS(@TempDir File tempDir) throws IOException {
        when(plugin.shouldDeflate()).thenReturn(false);

        int regionX = 0, regionZ = 0;

        RegionPersistor<BasicCustomLightSource> regionPersistor = new RegionPersistor<BasicCustomLightSource>(tempDir, regionX, regionZ, false) {
            @NotNull
            @Override
            protected BasicCustomLightSource[] createArray(int size) {
                return new BasicCustomLightSource[size];
            }

            @NotNull
            @Override
            protected BasicCustomLightSource[][] createMultiArr(int size) {
                return new BasicCustomLightSource[size][];
            }

            @NotNull
            @Override
            protected BasicCustomLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                return new BasicCustomLightSource(position, lightLevel, migrated, material);
            }
        };

        for (int z = 0; z < 32 * 16; ++z) {
            for (int x = 0; x < 32 * 16; ++x) {
                regionPersistor.put(new BasicCustomLightSource(new IntPosition(32 * 16 * regionX + x, 128, 32 * 16 * regionZ + z), 15, true, "minecraft:stone"));
            }
        }

        regionPersistor.flushAll();
        assertTrue(regionPersistor.save());
        assertTrue(new VLDBToNLSMigration(plugin).test(new File(tempDir, String.format(VLDBFile.FILE_NAME_FORMAT, regionX, regionZ))));

        NLSFile nlsFile = NLSFile.existingFile(new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, regionX, regionZ)), false);

        for (int z = 0; z < 32 * 16; ++z) {
            for (int x = 0; x < 32 * 16; ++x) {
                for (int y = 0; y < 256; ++y) {
                    assertEquals(y == 128 ? 15 : 0, nlsFile.getCustomLuminance(new IntPosition(32 * 16 * regionX + x, y, 32 * 16 * regionZ + z)));
                }
            }
        }

        nlsFile.unload();
    }

}
