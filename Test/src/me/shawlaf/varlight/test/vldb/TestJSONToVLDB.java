package me.shawlaf.varlight.test.vldb;

import com.google.gson.Gson;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.INmsAdapter;
import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.spigot.persistence.migrate.data.JsonToVLDBMigration;
import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJSONToVLDB {

    @Mock
    private VarLightPlugin plugin = mock(VarLightPlugin.class);

    @Mock
    private INmsAdapter nmsAdapter = mock(INmsAdapter.class);

    @Test
    public void testJSONToVLDBMigration(@TempDir File tempDir) throws IOException {
        when(nmsAdapter.materialToKey(Material.STONE)).thenReturn("minecraft:stone");
        when(plugin.getNmsAdapter()).thenReturn(nmsAdapter);

        final int regionX = 0, regionZ = 0;

        BasicCustomLightSource[] testData = new BasicCustomLightSource[16 * 32 * 16 * 32];
        int count = 0;

        for (int chunkX = 0; chunkX < 32; chunkX++) {
            for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        testData[count++] = new BasicCustomLightSource(new IntPosition((32 * regionX) + (chunkX * 16) + x, 128, (32 * regionZ) + (chunkZ * 16) + z), 15, true, "STONE");
                    }
                }
            }
        }

        File jsonFile = new File(tempDir, String.format("r.%d.%d.json", regionX, regionZ));

        Gson gson = new Gson();

        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(gson.toJson(testData));
        }

        assertTrue(new JsonToVLDBMigration(plugin).test(jsonFile));

        VLDBFile<BasicCustomLightSource> file = new VLDBFile<BasicCustomLightSource>(new File(tempDir, String.format(VLDBFile.FILE_NAME_FORMAT, regionX, regionZ)), true) {
            @NotNull
            @Override
            protected BasicCustomLightSource[] createArray(int size) {
                return new BasicCustomLightSource[size];
            }

            @NotNull
            @Override
            protected BasicCustomLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                return new BasicCustomLightSource(position, lightLevel, migrated, material);
            }
        };

        BasicCustomLightSource[] read = file.readAll().toArray(new BasicCustomLightSource[0]);
        BasicCustomLightSource[] testDataMigrated = new BasicCustomLightSource[testData.length];

        for (int i = 0; i < testDataMigrated.length; i++) {
            BasicCustomLightSource toMigrate = testData[i];

            testDataMigrated[i] = new BasicCustomLightSource(
                    toMigrate.getPosition(),
                    toMigrate.getCustomLuminance(),
                    toMigrate.isMigrated(),
                    plugin.getNmsAdapter().materialToKey(Material.valueOf(toMigrate.getType()))
            );
        }

        assertArrayEquals(testDataMigrated, read);
    }

}
