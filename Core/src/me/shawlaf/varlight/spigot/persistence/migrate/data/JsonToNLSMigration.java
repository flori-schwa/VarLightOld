package me.shawlaf.varlight.spigot.persistence.migrate.data;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.migrate.Migration;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.FileUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class JsonToNLSMigration implements Migration<File> {

    private final VarLightPlugin plugin;

    public JsonToNLSMigration(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @SneakyThrows(IOException.class)
    public boolean migrate(File jsonFile) {

        Objects.requireNonNull(jsonFile, "DB file may not be null!");

        if (!jsonFile.exists()) {
            throw new IllegalArgumentException("\"" + jsonFile.getAbsolutePath() + "\" does not exist!");
        }

        if (!".json".equalsIgnoreCase(FileUtil.getExtension(jsonFile))) {
            return false; // Ignore all non-json files
        }

        Gson gson = new Gson();
        BasicCustomLightSource[] jsonData;

        try (FileReader reader = new FileReader(jsonFile)) {
            jsonData = gson.fromJson(reader, BasicCustomLightSource[].class);
        }

        String fileInName = jsonFile.getName();
        fileInName = fileInName.substring(0, fileInName.lastIndexOf('.'));

        String[] partsFileInName = fileInName.substring(2).split("\\.");

        int regionX = Integer.parseInt(partsFileInName[0]);
        int regionZ = Integer.parseInt(partsFileInName[1]);

        File fileOut = new File(jsonFile.getParentFile().getAbsoluteFile(), fileInName + ".nls");

        NLSFile nlsFile = NLSFile.newFile(fileOut, regionX, regionZ, plugin.shouldDeflate());

        for (BasicCustomLightSource bcls : jsonData) {
            nlsFile.setCustomLuminance(bcls.getPosition(), bcls.getCustomLuminance());
        }

        nlsFile.saveAndUnload();

        if (!jsonFile.delete()) {
            throw new IOException("Failed to run migrations, could not delete File \"" + jsonFile.getAbsolutePath() + "\"");
        }

        return true;
    }
}
