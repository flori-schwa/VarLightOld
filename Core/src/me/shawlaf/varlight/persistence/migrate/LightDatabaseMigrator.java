package me.shawlaf.varlight.persistence.migrate;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class LightDatabaseMigrator {

    private static final List<Migration> MIGRATIONS = new ArrayList<>();

    static {
        MIGRATIONS.add(new JsonToVLDBMigration());
    }

    @NotNull
    private final File baseDir;

    public LightDatabaseMigrator(@NotNull File baseDir) {
        Objects.requireNonNull(baseDir, "BaseDir may not be null!");

        if (!baseDir.exists()) {
            throw new IllegalArgumentException("\"" + baseDir.getAbsolutePath() + "\" does not exist!");
        }

        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("\"" + baseDir.getAbsolutePath() + "\" is not a directory!");
        }

        this.baseDir = baseDir;
    }

    public void runMigrations(Logger logger) {
        boolean or;

        do {
            or = false;

            File[] files = baseDir.listFiles();

            if (files == null) {
                break;
            }

            for (File file : files) {
                for (Migration migration : MIGRATIONS) {
                    if (migration.migrate(file)) {
                        logger.info(String.format("[%s] Migrated File \"%s\"", migration.getClass().getSimpleName(), file.getAbsolutePath()));

                        or = true;

                        if (!file.delete()) {
                            throw new RuntimeException("Failed to run migrations, could not delete File \"" + file.getAbsolutePath() + "\"");
                        }
                    }
                }
            }
        } while (or);
    }
}
