package me.shawlaf.varlight.spigot.persistence.migrate;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.persistence.migrate.structure.MoveVarlightRootFolder;
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

    @NotNull
    private final World world;
    @NotNull
    private final VarLightPlugin plugin;

    public LightDatabaseMigrator(@NotNull VarLightPlugin plugin, @NotNull World world) {
        Objects.requireNonNull(plugin, "Plugin may not be null!");
        Objects.requireNonNull(world, "World may not be null!");

        this.world = world;
        this.plugin = plugin;
    }

    public static void addDataMigration(Predicate<File> migration) {
        DATA_MIGRATIONS.add(migration);
    }

    public static void addStructureMigration(Predicate<World> migration) {
        STRUCTURE_MIGRATIONS.add(migration);
    }

    public void runMigrations(Logger logger) {
        boolean or;

        do {
            or = false;

            for (Predicate<World> migration : STRUCTURE_MIGRATIONS) {
                try {
                    if (migration.test(world)) {
                        logger.info(String.format("[%s] Migrated World \"%s\"", migration.getClass().getSimpleName(), world.getName()));

                        or = true;
                    }
                } catch (Exception e) {
                    throw new MigrationFailedException(
                            String.format("Failed to Migrate World \"%s\": %s",
                                    world.getName(), e.getMessage()), e
                    );
                }
            }
        } while (or);


        File saveDir = plugin.getNmsAdapter().getVarLightSaveDirectory(world);

        do {
            or = false;

            File[] files = saveDir.listFiles();

            if (files == null) {
                break;
            }

            for (File file : files) {
                for (Predicate<File> migration : DATA_MIGRATIONS) {
                    try {
                        if (migration.test(file)) {
                            logger.info(String.format("[%s] Migrated File \"%s\"", migration.getClass().getSimpleName(), file.getAbsolutePath()));

                            or = true;
                        }
                    } catch (Exception e) {
                        throw new MigrationFailedException(
                                String.format("Failed to migrate file \"%s\" in world \"%s\": %s",
                                        file.getName(), world.getName(), e.getMessage()), e
                        );
                    }
                }
            }
        } while (or);
    }
}
