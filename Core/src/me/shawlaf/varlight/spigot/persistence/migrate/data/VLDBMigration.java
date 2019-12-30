package me.shawlaf.varlight.spigot.persistence.migrate.data;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.persistence.BasicCustomLightSource;
import me.shawlaf.varlight.persistence.vldb.VLDBInputStream;
import me.shawlaf.varlight.persistence.vldb.VLDBOutputStream;
import me.shawlaf.varlight.util.FileUtil;
import org.bukkit.Material;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class VLDBMigration implements Predicate<File> {

    private final VarLightPlugin plugin;

    public VLDBMigration(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean test(File file) {
        if (!file.getName().endsWith(".vldb")) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            VLDBInputStream in;

            if (VLDBInputStream.verifyVLDB(file)) {
                fis.close();

                return false;
            }

            if (FileUtil.isDeflated(file)) {
                in = new VLDBInputStream(new GZIPInputStream(fis));
            } else {
                in = new VLDBInputStream(fis); // this will probably never be the case, though
            }

            List<BasicCustomLightSource> old = in.readAll(BasicCustomLightSource[]::new, BasicCustomLightSource::new);

            List<BasicCustomLightSource> migratedLight = new ArrayList<>();

            for (BasicCustomLightSource lightSource : old) {
                migratedLight.add(
                        new BasicCustomLightSource(
                                lightSource.getPosition(),
                                lightSource.getCustomLuminance(),
                                lightSource.isMigrated(),
                                plugin.getNmsAdapter().materialToKey(Material.valueOf(lightSource.getType()))
                        )
                );
            }

            File newFile = new File(file.getAbsolutePath() + "2"); // r.X.Z.vldb -> r.X.Z.vldb2

            try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {
                VLDBOutputStream out;

                if (plugin.shouldVLDBDeflate()) {
                    out = new VLDBOutputStream(new GZIPOutputStream(fileOutputStream));
                } else {
                    out = new VLDBOutputStream(fileOutputStream);
                }

                out.write(migratedLight.toArray(new BasicCustomLightSource[0]));

                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Migration Failed: " + e.getMessage(), e);
        }

        if (!file.delete()) {
            throw new RuntimeException("Migration failed: Could not delete file " + file.getAbsolutePath());
        }

        return true;
    }

}
