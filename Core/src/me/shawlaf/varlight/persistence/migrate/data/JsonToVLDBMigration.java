package me.shawlaf.varlight.persistence.migrate.data;

import com.google.gson.Gson;
import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.vldb.VLDBOutputStream;
import me.shawlaf.varlight.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.zip.GZIPOutputStream;

public class JsonToVLDBMigration implements Predicate<File> {
    @Override
    public boolean test(File jsonFile) {

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
        } catch (IOException e) {
            throw new RuntimeException("Failed to migrate \"" + jsonFile.getAbsolutePath() + "\"", e);
        }

        String fileInName = jsonFile.getName();
        fileInName = fileInName.substring(0, fileInName.lastIndexOf('.'));

        File fileOut = new File(jsonFile.getParentFile().getAbsoluteFile(), fileInName + ".vldb");

        try (FileOutputStream fos = new FileOutputStream(fileOut)) {
            GZIPOutputStream gzipOut = new GZIPOutputStream(fos);
            VLDBOutputStream out = new VLDBOutputStream(gzipOut);

            out.write(jsonData);

            out.flush();
            gzipOut.flush();
            gzipOut.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to migrate \"" + jsonFile.getAbsolutePath() + "\"", e);
        }

        if (!jsonFile.delete()) {
            throw new RuntimeException("Failed to run migrations, could not delete File \"" + jsonFile.getAbsolutePath() + "\"");
        }

        return true;
    }
}
