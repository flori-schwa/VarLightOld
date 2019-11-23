package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.persistence.migrate.LightDatabaseMigrator;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldLightSourceManager {

    public static final String TAG_WORLD_LIGHT_SOURCE_PERSISTOR = "varlight:persistor";

    private final Map<RegionCoords, RegionPersistor<PersistentLightSource>> worldMap;
    private final VarLightPlugin plugin;
    private final World world;

    // region static

    public static List<WorldLightSourceManager> getAllManager(VarLightPlugin plugin) {
        List<WorldLightSourceManager> list = new ArrayList<>();

        for (World w : Bukkit.getWorlds()) {
            WorldLightSourceManager persistor = getManager(plugin, w);

            if (persistor != null) {
                list.add(persistor);
            }
        }

        return list;
    }

    public static WorldLightSourceManager createManager(VarLightPlugin plugin, World world) {
        if (world.hasMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR)) {
            for (MetadataValue m : world.getMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR)) {
                if (m.getOwningPlugin().equals(plugin)) {
                    return (WorldLightSourceManager) Optional.of(m).map(MetadataValue::value).orElse(null);
                }
            }

            return (WorldLightSourceManager) Optional.<MetadataValue>empty().map(MetadataValue::value).orElse(null);
        }

        FixedMetadataValue fixedMetadataValue = new FixedMetadataValue(plugin, new WorldLightSourceManager(plugin, world));
        world.setMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR, fixedMetadataValue);

        return (WorldLightSourceManager) fixedMetadataValue.value();
    }

    public static boolean hasManager(VarLightPlugin plugin, World world) {
        return getManager(plugin, world) != null;
    }

    @Nullable
    public static WorldLightSourceManager getManager(VarLightPlugin plugin, World world) {
        for (MetadataValue metadataValue : world.getMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR)) {
            if (metadataValue.getOwningPlugin().equals(plugin)) {
                return (WorldLightSourceManager) metadataValue.value();
            }
        }

        return null;
    }

    public static int getLuminance(VarLightPlugin plugin, Location location) {
        WorldLightSourceManager manager = getManager(plugin, location.getWorld());

        if (manager == null) {
            return plugin.getNmsAdapter().getEmittingLightLevel(location.getBlock());
        }

        PersistentLightSource lightSource = manager.getPersistentLightSource(location);

        if (lightSource == null) {
            return plugin.getNmsAdapter().getEmittingLightLevel(location.getBlock());
        }

        return lightSource.getEmittingLight();
    }

    // endregion

    private WorldLightSourceManager(VarLightPlugin plugin, World world) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);

        plugin.getLogger().info(String.format("Created a new Lightsource Persistor for world \"%s\"", world.getName()));

        this.worldMap = new HashMap<>();
        this.plugin = plugin;
        this.world = world;

        synchronized (worldMap) {
            File varlightFolder = new File(world.getWorldFolder(), "varlight");

            if (!varlightFolder.exists()) {
                if (!varlightFolder.mkdir()) {
                    throw new LightPersistFailedException("Could not create varlight directory in world \"" + world.getName() + "\"");
                }
            }

            new LightDatabaseMigrator(varlightFolder).runMigrations(plugin.getLogger());

            for (Chunk chunk : world.getLoadedChunks()) {
                loadChunk(chunk);
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

    public void setCustomLuminance(IntPosition position, int lightLevel) {
        createPersistentLightSource(position, lightLevel);
//        getOrCreatePersistentLightSource(position).setEmittingLight(lightLevel);
    }

    public void loadChunk(Chunk chunk) {
        try {
            getRegionPersistor(new RegionCoords(chunk.getX() >> 5, chunk.getZ() >> 5)).loadChunk(new ChunkCoords(chunk.getX(), chunk.getZ()));
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }
    }

    public void unloadChunk(Chunk chunk) {
        try {
            getRegionPersistor(new RegionCoords(chunk.getX() >> 5, chunk.getZ() >> 5)).unloadChunk(new ChunkCoords(chunk.getX(), chunk.getZ()));
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }
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

    @NotNull
    public List<PersistentLightSource> getAllLightSources() {
        File[] files = getSaveDirectory().listFiles();

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
        int persistedRegions = 0;
        List<RegionCoords> regionsToUnload = new ArrayList<>();

        synchronized (worldMap) {
            for (RegionPersistor<PersistentLightSource> persistor : worldMap.values()) {
                int loaded = 0;

                try {
                    persistor.flushAll();
                    persistor.save();
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                List<ChunkCoords> affected = persistor.getAffectedChunks();

                if (affected.size() == 0) {
                    if (!persistor.file.delete()) {
                        throw new LightPersistFailedException("Could not delete file " + persistor.file.file.getAbsolutePath());
                    }

                    continue;
                } else {
                    persistedRegions++;
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
            commandSender.sendMessage(String.format("[VarLight] Light Sources persisted for World \"%s\", Files written: %d", world.getName(), persistedRegions));
        }
    }

    private void unloadRegion(RegionCoords key) {
        synchronized (worldMap) {
            worldMap.remove(key);
        }
    }

    @Nullable
    private PersistentLightSource getPersistentLightSource(IntPosition intPosition) {
        RegionPersistor<PersistentLightSource> regionMap = getRegionPersistor(new RegionCoords(intPosition));

        PersistentLightSource persistentLightSource = regionMap.getLightSource(intPosition);

        if (persistentLightSource != null) {
            persistentLightSource.update();

            if (!persistentLightSource.isValid()) {
                persistentLightSource = null;
            }
        }

        return persistentLightSource;
    }

    @Nullable
    private PersistentLightSource getPersistentLightSource(Location location) {
        return getPersistentLightSource(new IntPosition(location));
    }

    @NotNull
    private PersistentLightSource createPersistentLightSource(IntPosition intPosition, int emittingLight) {
        PersistentLightSource persistentLightSource = new PersistentLightSource(plugin, world, intPosition, emittingLight);
        persistentLightSource.migrated = plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2);

        try {
            getRegionPersistor(new RegionCoords(intPosition)).put(persistentLightSource);
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }

        return persistentLightSource;
    }

    private RegionPersistor<PersistentLightSource> getRegionPersistor(RegionCoords regionCoords) {
        synchronized (worldMap) {
            if (!worldMap.containsKey(regionCoords)) {
                try {
                    worldMap.put(regionCoords, new RegionPersistor<PersistentLightSource>(getSaveDirectory(), regionCoords.x, regionCoords.z) {
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

    private File getSaveDirectory() {
        File varlightDir = new File(world.getWorldFolder(), "varlight");

        if (!varlightDir.exists()) {
            if (!varlightDir.mkdir()) {
                throw new LightPersistFailedException();
            }
        }

        return varlightDir;
    }
}
