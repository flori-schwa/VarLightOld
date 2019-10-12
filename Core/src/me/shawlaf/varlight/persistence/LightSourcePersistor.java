package me.shawlaf.varlight.persistence;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoordinates;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class LightSourcePersistor {

    public static final String TAG_WORLD_LIGHT_SOURCE_PERSISTOR = "varlight:persistor";
    private final Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> worldMap;
    private final VarLightPlugin plugin;
    private final World world;

    private LightSourcePersistor(VarLightPlugin plugin, World world) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);

        plugin.getLogger().info(String.format("Created a new Lightsource Persistor for world \"%s\"", world.getName()));

        this.worldMap = new HashMap<>();
        this.plugin = plugin;
        this.world = world;
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
        Map<IntPosition, PersistentLightSource> regionMap = getRegionMap(new RegionCoordinates(intPosition));
        PersistentLightSource persistentLightSource = regionMap.get(intPosition);

        if (persistentLightSource != null) {
            persistentLightSource.update();

            if (!persistentLightSource.isValid()) {
                persistentLightSource = null;
            }
        }

        return persistentLightSource;
    }

    public PersistentLightSource createPersistentLightSource(IntPosition intPosition, int emittingLight) {
        PersistentLightSource persistentLightSource = new PersistentLightSource(plugin, world, intPosition, emittingLight);
        persistentLightSource.migrated = plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2);

        getRegionMap(new RegionCoordinates(intPosition)).put(intPosition, persistentLightSource);

        return persistentLightSource;
    }

    public PersistentLightSource getOrCreatePersistentLightSource(IntPosition position) {
        return Optional.ofNullable(getPersistentLightSource(position))
                .orElseGet(() -> createPersistentLightSource(position, 0));
    }

    public Stream<PersistentLightSource> getAllLightSources() {

        File[] files = getSaveDirectory().listFiles();

        if (files != null) {
            for (File regionFile : files) {
                try {
                    String name = regionFile.getName().substring(2, regionFile.getName().length() - ".json".length());
                    String[] coords = name.split("\\.");

                    RegionCoordinates regionCoordinates = new RegionCoordinates(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                    getRegionMap(regionCoordinates);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return worldMap.values().stream().map(Map::values).flatMap(Collection::stream);
    }

    public void save(CommandSender commandSender) {
        synchronized (world) {
            Gson gson = new Gson();

            int persistedRegions = 0;

            List<RegionCoordinates> regionsToUnload = new ArrayList<>();

            for (Map.Entry<RegionCoordinates, Map<IntPosition, PersistentLightSource>> entry : worldMap.entrySet()) {
                File saveFile = getSaveFile(entry.getKey());
                List<IntPosition> lightSourcesToUnload = new ArrayList<>();

                int written = 0, loaded = 0;

                try (JsonWriter jsonWriter = new JsonWriter(new FileWriter(saveFile))) {
                    jsonWriter.beginArray();

                    for (PersistentLightSource persistentLightSource : entry.getValue().values()) {
                        if (world.isChunkLoaded(persistentLightSource.getPosition().getChunkX(), persistentLightSource.getPosition().getChunkZ())) {
                            loaded++;
                        } else {
                            written++;
                            persistentLightSource.write(gson, jsonWriter);
                            lightSourcesToUnload.add(persistentLightSource.getPosition());
                            continue;
                        }

                        if (persistentLightSource.isValid()) {
                            persistentLightSource.write(gson, jsonWriter);
                            written++;
                        }
                    }

                    jsonWriter.endArray();
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                for (IntPosition intPosition : lightSourcesToUnload) {
                    entry.getValue().remove(intPosition);
                }

                if (written == 0) {
                    saveFile.delete();
                } else {
                    persistedRegions++;
                }

                if (loaded == 0) {
                    regionsToUnload.add(entry.getKey());
                }
            }

            for (RegionCoordinates regionCoordinates : regionsToUnload) {
                unloadRegion(regionCoordinates);
            }

            if (plugin.getConfiguration().isLoggingPersist() || commandSender instanceof Player) { // Players will still receive the message if manually triggering a save
                commandSender.sendMessage(String.format("[VarLight] Light Sources persisted for World \"%s\", Files written: %d", world.getName(), persistedRegions));
            }
        }
    }

    private void unloadRegion(RegionCoordinates key) {
        worldMap.remove(key);
    }

    private Map<IntPosition, PersistentLightSource> getRegionMap(RegionCoordinates regionCoordinates) {
        if (!worldMap.containsKey(regionCoordinates)) {

            File regionFile = getSaveFile(regionCoordinates);

            if (!regionFile.exists()) {
                worldMap.put(regionCoordinates, new HashMap<>());
            } else {
                worldMap.put(regionCoordinates, loadRegionMap(regionFile));
            }
        }

        return worldMap.get(regionCoordinates);
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

    private File getSaveFile(RegionCoordinates regionCoordinates) {
        return new File(getSaveDirectory(), String.format("r.%d.%d.json", regionCoordinates.getRegionX(), regionCoordinates.getRegionZ()));
    }

    private Map<IntPosition, PersistentLightSource> loadRegionMap(File file) {
        Map<IntPosition, PersistentLightSource> regionMap = new HashMap<>();
        Gson gson = new Gson();

        try (JsonReader jsonReader = new JsonReader(new FileReader(file))) {
            jsonReader.beginArray();

            while (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                PersistentLightSource persistentLightSource = PersistentLightSource.read(gson, jsonReader);
                persistentLightSource.initialize(world, plugin);

                regionMap.put(persistentLightSource.getPosition(), persistentLightSource);
            }

            jsonReader.endArray();
        } catch (IOException e) {
            throw new LightPersistFailedException(e);
        } catch (IllegalStateException e) {
            throw new LightPersistFailedException(String.format("Failed to load %s: Malformed Region File", file.getName()), e);
        }

        return regionMap;
    }
}
