package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.persistence.vldb.VLDBFile;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RegionPersistor {

    private final int regionX, regionZ;
    private final World world;
    private final VarLightPlugin plugin;

    private VLDBFile<PersistentLightSource> file;
    private Map<ChunkCoords, List<PersistentLightSource>> chunkCache = new HashMap<>();

    public RegionPersistor(VarLightPlugin plugin, File vldbRoot, World world, int regionX, int regionZ) throws IOException {
        Objects.requireNonNull(vldbRoot);

        this.world = Objects.requireNonNull(world);
        this.plugin = Objects.requireNonNull(plugin);

        if (!vldbRoot.exists()) {
            vldbRoot.mkdir();
        }

        if (!vldbRoot.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not a directory!", vldbRoot.getAbsolutePath()));
        }

        this.regionX = regionX;
        this.regionZ = regionZ;

        File vldbFile = new File(vldbRoot, String.format(VLDBFile.FILE_NAME_FORMAT, regionX, regionZ));

        if (!vldbFile.exists()) {
            this.file = new VLDBFile<PersistentLightSource>(vldbFile, regionX, regionZ) {
                @NotNull
                @Override
                protected PersistentLightSource[] createArray(int size) {
                    return new PersistentLightSource[size];
                }

                @NotNull
                @Override
                protected PersistentLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return new PersistentLightSource(position, Material.valueOf(material), migrated, world, plugin, lightLevel);
                }
            };
        } else {
            this.file = new VLDBFile<PersistentLightSource>(vldbFile) {
                @NotNull
                @Override
                protected PersistentLightSource[] createArray(int size) {
                    return new PersistentLightSource[size];
                }

                @NotNull
                @Override
                protected PersistentLightSource createInstance(IntPosition position, int lightLevel, boolean migrated, String material) {
                    return new PersistentLightSource(position, Material.valueOf(material), migrated, world, plugin, lightLevel);
                }
            };
        }

    }
}