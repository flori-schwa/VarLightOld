package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command_old.VarLightCommand;
import me.shawlaf.varlight.persistence.migrate.LightDatabaseMigrator;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldLightSourceManager {

    private final Map<RegionCoords, RegionPersistor<PersistentLightSource>> worldMap;
    private final VarLightPlugin plugin;
    private final World world;

    private long lastMigrateNotice = 0;

    public WorldLightSourceManager(VarLightPlugin plugin, World world) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);

        plugin.getLogger().info(String.format("Created a new Lightsource Persistor for world \"%s\"", world.getName()));

        this.worldMap = new HashMap<>();
        this.plugin = plugin;
        this.world = world;

        synchronized (worldMap) {
            File varlightFolder = getVarLightSaveDirectory(world);

            new LightDatabaseMigrator(world).runMigrations(plugin.getLogger());

            if (!varlightFolder.exists()) {
                if (!varlightFolder.mkdir()) {
                    throw new LightPersistFailedException("Could not create varlight directory in world \"" + world.getName() + "\"");
                }
            }
        }
    }

    public static File getVarLightSaveDirectory(World world) {
        File varlightDir = new File(getRegionRoot(world), "varlight");

        if (!varlightDir.exists()) {
            if (!varlightDir.mkdir()) {
                throw new LightPersistFailedException();
            }
        }

        return varlightDir;
    }

    public static File getRegionRoot(World world) {
        switch (world.getEnvironment()) {
            case NORMAL: {
                return world.getWorldFolder();
            }

            case NETHER: {
                return new File(world.getWorldFolder(), "DIM-1");
            }

            case THE_END: {
                return new File(world.getWorldFolder(), "DIM1");
            }

            default: {
                throw new IllegalStateException("wot");
            }
        }
    }

    public VarLightPlugin getPlugin() {
        return plugin;
    }

    public World getWorld() {
        return world;
    }

    public int getCustomLuminance(IntPosition position, int def) {
        return getCustomLuminance(position, () -> def);
    }

    public int getCustomLuminance(IntPosition position, IntSupplier def) {
        PersistentLightSource lightSource = getPersistentLightSource(position);

        if (lightSource == null) {
            return def.getAsInt();
        }

        return lightSource.getEmittingLight();
    }

    public void setCustomLuminance(Location location, int lightLevel) {
        setCustomLuminance(new IntPosition(location), lightLevel);
    }

//    @NotNull
//    private PersistentLightSource getOrCreatePersistentLightSource(IntPosition position) {
//        PersistentLightSource persistentLightSource = getPersistentLightSource(position);
//
//        if (persistentLightSource == null) {
//            persistentLightSource = createPersistentLightSource(position, 0);
//        }
//
//        return persistentLightSource;
//    }

    public void setCustomLuminance(IntPosition position, int lightLevel) {
        createPersistentLightSource(position, lightLevel);
//        getOrCreatePersistentLightSource(position).setEmittingLight(lightLevel);
    }

    //    public void loadChunk(Chunk chunk) {
//        try {
//            getRegionPersistor(new RegionCoords(chunk.getX() >> 5, chunk.getZ() >> 5)).loadChunk(new ChunkCoords(chunk.getX(), chunk.getZ()));
//        } catch (IOException e) {
//            throw new LightPersistFailedException(e);
//        }
//    }
//
    public void unloadChunk(Chunk chunk) {
        try {
            getRegionPersistor(new RegionCoords(chunk.getX() >> 5, chunk.getZ() >> 5)).unloadChunk(new ChunkCoords(chunk.getX(), chunk.getZ()));
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }
    }

    @NotNull
    public List<PersistentLightSource> getAllLightSources() {
        File[] files = getVarLightSaveDirectory(world).listFiles();

        if (files == null) {
            synchronized (worldMap) {
                return worldMap.values().stream().flatMap(regionPersistor -> {
                    try {
                        return regionPersistor.loadAll().stream();
                    } catch (IOException e) {
                        e.printStackTrace();

                        return Stream.empty();
                    }
                }).collect(Collectors.toList());
            }
        }

        for (File regionFile : files) {
            try {
                String name = regionFile.getName().substring(2, regionFile.getName().length() - ".json".length());
                String[] coords = name.split("\\.");

                RegionCoords regionCoords = new RegionCoords(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                getRegionPersistor(regionCoords);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        synchronized (worldMap) {
            return worldMap.values().stream().flatMap(regionPersistor -> {
                try {
                    return regionPersistor.loadAll().stream();
                } catch (IOException e) {
                    e.printStackTrace();

                    return Stream.empty();
                }
            }).collect(Collectors.toList());
        }
    }

    public void save(CommandSender commandSender) {
        int modified = 0, deleted = 0;
        List<RegionCoords> regionsToUnload = new ArrayList<>();

        synchronized (worldMap) {
            for (RegionPersistor<PersistentLightSource> persistor : worldMap.values()) {
                int loaded = 0;

                try {
                    persistor.flushAll();
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                List<ChunkCoords> affected = persistor.getAffectedChunks();

                if (affected.size() == 0) {
                    if (persistor.file.file.exists()) {
                        if (!persistor.file.delete()) {
                            throw new LightPersistFailedException("Could not delete file " + persistor.file.file.getAbsolutePath());
                        } else {
                            if (plugin.getConfiguration().isLoggingPersist() || commandSender instanceof Player) {
                                VarLightCommand.sendPrefixedMessage(commandSender, String.format("Deleted File %s", persistor.file.file.getName()));
                            }

                            deleted++;
                        }
                    }

                    regionsToUnload.add(new RegionCoords(persistor.regionX, persistor.regionZ));
                    continue;
                } else {
                    try {
                        if (persistor.save()) {
                            modified++;
                        }
                    } catch (IOException e) {
                        throw new LightPersistFailedException(e);
                    }
                }

                for (ChunkCoords chunkCoords : affected) {
                    if (world.isChunkLoaded(chunkCoords.x, chunkCoords.z)) {
                        loaded++;
                    }
                }

                if (loaded == 0) {
                    regionsToUnload.add(new RegionCoords(persistor.regionX, persistor.regionZ));
                }
            }

            for (RegionCoords regionCoords : regionsToUnload) {
                unloadRegion(regionCoords);
            }
        }

        if (plugin.getConfiguration().isLoggingPersist() || commandSender instanceof Player) { // Players will still receive the message if manually triggering a save
            commandSender.sendMessage(String.format("[VarLight] Light Sources persisted for World \"%s\", Files modified: %d, Files deleted: %d", world.getName(), modified, deleted));
        }
    }

    private void unloadRegion(RegionCoords key) {
        synchronized (worldMap) {
            worldMap.remove(key).unload();
        }
    }

    @Nullable
    private PersistentLightSource getPersistentLightSource(IntPosition intPosition) {
        RegionPersistor<PersistentLightSource> regionMap = getRegionPersistor(new RegionCoords(intPosition));

        PersistentLightSource persistentLightSource;

        try {
            persistentLightSource = regionMap.getLightSource(intPosition);
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }

        if (persistentLightSource != null &&
                !persistentLightSource.migrated &&
                plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2)) {
            if ((System.currentTimeMillis() - lastMigrateNotice) > 30_000) {

                Bukkit.broadcast(
                        VarLightCommand.getPrefixedMessage(String.format("There are non-migrated Light sources present in world \"%s\", please run /varlight migrate!", world.getName())),
                        "varlight.admin"
                );

                lastMigrateNotice = System.currentTimeMillis();
            }
        }

        return persistentLightSource;
    }

    @Nullable
    public PersistentLightSource getPersistentLightSource(Location location) {
        return getPersistentLightSource(new IntPosition(location));
    }

    @NotNull
    public PersistentLightSource createPersistentLightSource(IntPosition intPosition, int emittingLight) {
        PersistentLightSource persistentLightSource = new PersistentLightSource(plugin, world, intPosition, emittingLight);
        persistentLightSource.migrated = plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2);

        try {
            getRegionPersistor(new RegionCoords(intPosition)).put(persistentLightSource);
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }

        return persistentLightSource;
    }

    @NotNull
    public RegionPersistor<PersistentLightSource> getRegionPersistor(RegionCoords regionCoords) {
        synchronized (worldMap) {
            if (!worldMap.containsKey(regionCoords)) {
                try {
                    worldMap.put(regionCoords, new RegionPersistor<PersistentLightSource>(getVarLightSaveDirectory(world), regionCoords.x, regionCoords.z) {
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
                    });
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }
            }

            return worldMap.get(regionCoords);
        }
    }
}
