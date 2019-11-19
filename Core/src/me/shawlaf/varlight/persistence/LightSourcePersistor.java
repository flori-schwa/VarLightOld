package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.persistence.migrate.LightDatabaseMigrator;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoordinates;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LightSourcePersistor {

    public static final String TAG_WORLD_LIGHT_SOURCE_PERSISTOR = "varlight:persistor";
    private final Map<RegionCoordinates, RegionPersistor<PersistentLightSource>> worldMap;
    private final VarLightPlugin plugin;
    private final World world;

    private LightSourcePersistor(VarLightPlugin plugin, World world) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);

        plugin.getLogger().info(String.format("Created a new Lightsource Persistor for world \"%s\"", world.getName()));

        this.worldMap = new HashMap<>();
        this.plugin = plugin;
        this.world = world;

        synchronized (worldMap) {
            File varlightFolder = new File(world.getWorldFolder(), "varlight");

            if (!varlightFolder.exists()) {
                if (varlightFolder.mkdir()) {
                    throw new LightPersistFailedException("Could not create varlight directory in world \"" + world.getName() + "\"");
                }
            }

            new LightDatabaseMigrator(varlightFolder).runMigrations(plugin.getLogger());
        }
    }

    public static List<LightSourcePersistor> getAllPersistors(VarLightPlugin plugin) {
        List<LightSourcePersistor> list = new ArrayList<>();

        for (World w : Bukkit.getWorlds()) {
            LightSourcePersistor persistor = getPersistor(plugin, w);

            if (persistor != null) {
                list.add(persistor);
            }
        }

        return list;
    }

    public static LightSourcePersistor createPersistor(VarLightPlugin plugin, World world) {
        if (world.hasMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR)) {
            for (MetadataValue m : world.getMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR)) {
                if (m.getOwningPlugin().equals(plugin)) {
                    return (LightSourcePersistor) Optional.of(m).map(MetadataValue::value).orElse(null);
                }
            }

            return (LightSourcePersistor) Optional.<MetadataValue>empty().map(MetadataValue::value).orElse(null);
        }

        FixedMetadataValue fixedMetadataValue = new FixedMetadataValue(plugin, new LightSourcePersistor(plugin, world));
        world.setMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR, fixedMetadataValue);

        return (LightSourcePersistor) fixedMetadataValue.value();
    }

    public static boolean hasPersistor(VarLightPlugin plugin, World world) {
        return getPersistor(plugin, world) != null;
    }

    @Nullable
    public static LightSourcePersistor getPersistor(VarLightPlugin plugin, World world) {
        for (MetadataValue metadataValue : world.getMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR)) {
            if (metadataValue.getOwningPlugin().equals(plugin)) {
                return (LightSourcePersistor) metadataValue.value();
            }
        }

        return null;
    }

    public static int getEmittingLightLevel(VarLightPlugin plugin, Location location) {
        LightSourcePersistor persistor = getPersistor(plugin, location.getWorld());

        if (persistor == null) {
            return plugin.getNmsAdapter().getEmittingLightLevel(location.getBlock());
        }

        PersistentLightSource lightSource = persistor.getPersistentLightSource(location);

        if (lightSource == null) {
            return plugin.getNmsAdapter().getEmittingLightLevel(location.getBlock());
        }

        return lightSource.getEmittingLight();
    }

    public VarLightPlugin getPlugin() {
        return plugin;
    }

    public World getWorld() {
        return world;
    }

    public int getEmittingLightLevel(Location location, int def) {

        PersistentLightSource lightSource = getPersistentLightSource(location);

        if (lightSource == null) {
            return def;
        }

        return lightSource.getEmittingLight();
    }

    @Nullable
    public PersistentLightSource getPersistentLightSource(Block block) {
        return getPersistentLightSource(block.getLocation());
    }

    @Nullable
    public PersistentLightSource getPersistentLightSource(Location location) {
        return getPersistentLightSource(new IntPosition(location));
    }

    @Nullable
    public PersistentLightSource getPersistentLightSource(int x, int y, int z) {
        return getPersistentLightSource(new IntPosition(x, y, z));
    }

    @Nullable
    public PersistentLightSource getPersistentLightSource(IntPosition intPosition) {
        RegionPersistor<PersistentLightSource> regionMap;

        regionMap = getRegionPersistor(new RegionCoordinates(intPosition));

        PersistentLightSource persistentLightSource = regionMap.getLightSource(intPosition);

        if (persistentLightSource != null) {
            persistentLightSource.update();

            if (!persistentLightSource.isValid()) {
                persistentLightSource = null;
            }
        }

        return persistentLightSource;
    }

    @NotNull
    public PersistentLightSource createPersistentLightSource(IntPosition intPosition, int emittingLight) {
        PersistentLightSource persistentLightSource = new PersistentLightSource(plugin, world, intPosition, emittingLight);
        persistentLightSource.migrated = plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2);

        try {
            getRegionPersistor(new RegionCoordinates(intPosition)).put(persistentLightSource);
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        }

        return persistentLightSource;
    }

    @NotNull
    public PersistentLightSource getOrCreatePersistentLightSource(IntPosition position) {
        PersistentLightSource persistentLightSource = getPersistentLightSource(position);

        if (persistentLightSource == null) {
            persistentLightSource = createPersistentLightSource(position, 0);
        }

        return persistentLightSource;
    }

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

                RegionCoordinates regionCoordinates = new RegionCoordinates(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                getRegionPersistor(regionCoordinates);
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
        List<RegionCoordinates> regionsToUnload = new ArrayList<>();

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

                    regionsToUnload.add(new RegionCoordinates(persistor.regionX, persistor.regionZ));
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
                    regionsToUnload.add(new RegionCoordinates(persistor.regionX, persistor.regionZ));
                }
            }

            for (RegionCoordinates regionCoordinates : regionsToUnload) {
                unloadRegion(regionCoordinates);
            }
        }

        if (plugin.getConfiguration().isLoggingPersist() || commandSender instanceof Player) { // Players will still receive the message if manually triggering a save
            commandSender.sendMessage(String.format("[VarLight] Light Sources persisted for World \"%s\", Files written: %d", world.getName(), persistedRegions));
        }
    }

    private void unloadRegion(RegionCoordinates key) {
        synchronized (worldMap) {
            worldMap.remove(key);
        }
    }

    private RegionPersistor<PersistentLightSource> getRegionPersistor(RegionCoordinates regionCoordinates) {
        synchronized (worldMap) {
            if (!worldMap.containsKey(regionCoordinates)) {
                try {
                    worldMap.put(regionCoordinates, new RegionPersistor<PersistentLightSource>(getSaveDirectory(), regionCoordinates.x, regionCoordinates.z) {
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

            return worldMap.get(regionCoordinates);
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
