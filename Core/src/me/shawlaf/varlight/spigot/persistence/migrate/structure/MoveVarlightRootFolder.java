package me.shawlaf.varlight.spigot.persistence.migrate.structure;

import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Predicate;

public class MoveVarlightRootFolder implements Predicate<World> {

    @NotNull
    private final VarLightPlugin plugin;

    public MoveVarlightRootFolder(@NotNull VarLightPlugin plugin) {
        Objects.requireNonNull(plugin, "Plugin may not be null");
        this.plugin = plugin;
    }


    @Override
    public boolean test(World world) {
        if (world.getEnvironment() == World.Environment.NORMAL) {
            return false;
        }

        File oldVarLightFolder = new File(world.getWorldFolder(), "varlight");
        File newVarLightFolder = plugin.getNmsAdapter().getVarLightSaveDirectory(world);

        if (oldVarLightFolder.equals(newVarLightFolder)) {
            return false;
        }

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
