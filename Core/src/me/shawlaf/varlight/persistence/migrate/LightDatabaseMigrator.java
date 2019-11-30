package me.shawlaf.varlight.persistence.migrate;

import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.persistence.migrate.data.JsonToVLDBMigration;
import me.shawlaf.varlight.persistence.migrate.structure.MoveVarlightRootFolder;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class LightDatabaseMigrator {

    private static final List<Predicate<File>> DATA_MIGRATIONS = new ArrayList<>();
    private static final List<Predicate<World>> STRUCTURE_MIGRATIONS = new ArrayList<>();

    static {
        DATA_MIGRATIONS.add(new JsonToVLDBMigration());

        STRUCTURE_MIGRATIONS.add(new MoveVarlightRootFolder());
    }

    @NotNull
    private final World world;

    public LightDatabaseMigrator(@NotNull World world) {
        Objects.requireNonNull(world, "World may not be null!");
        this.world = world;
    }

    public void runMigrations(Logger logger) {
        boolean or;

        do {
            or = false;

            for (Predicate<World> migration : STRUCTURE_MIGRATIONS) {
                if (migration.test(world)) {
                    logger.info(String.format("[%s] Migrated World \"%s\"", migration.getClass().getSimpleName(), world.getName()));

                    or = true;
                }
            }
        } while (or);


        File saveDir = WorldLightSourceManager.getVarLightSaveDirectory(world);

        do {
            or = false;

            File[] files = saveDir.listFiles();

            if (files == null) {
                break;
            }

            for (File file : files) {
                for (Predicate<File> migration : DATA_MIGRATIONS) {
                    if (migration.test(file)) {
                        logger.info(String.format("[%s] Migrated File \"%s\"", migration.getClass().getSimpleName(), file.getAbsolutePath()));

                        or = true;
                    }
                }
            }
        } while (or);
    }
}
