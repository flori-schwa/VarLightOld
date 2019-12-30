package me.shawlaf.varlight.spigot.persistence.migrate.structure;

import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

public class MoveVarlightRootFolder implements Predicate<World> {
    @Override
    public boolean test(World world) {
        if (world.getEnvironment() == World.Environment.NORMAL) {
            return false;
        }

        File oldVarLightFolder = new File(world.getWorldFolder(), "varlight");
        File newVarLightFolder = WorldLightSourceManager.getVarLightSaveDirectory(world);

        if (oldVarLightFolder.exists() && oldVarLightFolder.isDirectory()) {
            File[] files = oldVarLightFolder.listFiles();

            if (files != null && files.length > 0) {
                try {
                    Files.move(oldVarLightFolder.toPath(), newVarLightFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }
            } else {
                return oldVarLightFolder.delete();
            }
        }

        return false;
    }
}
