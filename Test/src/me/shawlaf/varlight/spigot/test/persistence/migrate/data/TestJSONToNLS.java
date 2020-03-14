package me.shawlaf.varlight.spigot.test.persistence.migrate.data;

import com.google.gson.Gson;
import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.INmsAdapter;
import me.shawlaf.varlight.spigot.persistence.migrate.data.JsonToNLSMigration;
import me.shawlaf.varlight.util.IntPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.io.FileWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJSONToNLS {

    @Mock
    private VarLightPlugin plugin = mock(VarLightPlugin.class);

    @Mock
    private INmsAdapter nmsAdapter = mock(INmsAdapter.class);

    @Test
    public void testJSONToVLDBMigration(@TempDir File tempDir) throws Exception {
        when(plugin.getNmsAdapter()).thenReturn(nmsAdapter);
        when(plugin.shouldDeflate()).thenReturn(false);

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

        assertTrue(new JsonToNLSMigration(plugin).migrate(jsonFile));

        NLSFile nlsFile = NLSFile.existingFile(new File(tempDir, String.format(NLSFile.FILE_NAME_FORMAT, regionX, regionZ)));

        for (int dz = 0; dz < 32 * 16; ++dz) {
            for (int dx = 0; dx < 32 * 16; ++dx) {
                assertEquals(15, nlsFile.getCustomLuminance(new IntPosition(regionX * 32 * 16 + dx, 128, regionZ * 32 * 16 + dz)));
            }
        }
    }

}
