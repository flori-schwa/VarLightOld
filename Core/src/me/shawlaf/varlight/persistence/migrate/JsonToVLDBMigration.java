package me.shawlaf.varlight.persistence.migrate;

import com.google.gson.Gson;
import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.vldb.VLDBOutputStream;
import me.shawlaf.varlight.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class JsonToVLDBMigration implements Migration {
    @Override
    public boolean migrate(File dataBaseFile) {

        Objects.requireNonNull(dataBaseFile, "DB file may not be null!");

        if (!dataBaseFile.exists()) {
            throw new IllegalArgumentException("\"" + dataBaseFile.getAbsolutePath() + "\" does not exist!");
        }

        if (!".json".equalsIgnoreCase(FileUtil.getExtension(dataBaseFile))) {
            return false; // Ignore all non-json files
        }

        Gson gson = new Gson();
        BasicCustomLightSource[] jsonData;

        try (FileReader reader = new FileReader(dataBaseFile)) {
            jsonData = gson.fromJson(reader, BasicCustomLightSource[].class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to migrate \"" + dataBaseFile.getAbsolutePath() + "\"", e);
        }

        String fileInName = dataBaseFile.getName();
        fileInName = fileInName.substring(0, fileInName.lastIndexOf('.'));

        File fileOut = new File(dataBaseFile.getParentFile().getAbsoluteFile(), fileInName + ".vldb");

        try (VLDBOutputStream out = new VLDBOutputStream(new FileOutputStream(fileOut))) {
            out.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to migrate \"" + dataBaseFile.getAbsolutePath() + "\"", e);
        }

        return true;
    }
}
