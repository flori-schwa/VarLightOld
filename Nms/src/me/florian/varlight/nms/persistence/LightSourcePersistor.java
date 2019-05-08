package me.florian.varlight.nms.persistence;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.florian.varlight.IntPosition;
import me.florian.varlight.RegionCoordinates;
import me.florian.varlight.VarLightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LightSourcePersistor {

    public static final Logger LOGGER = Logger.getLogger(LightSourcePersistor.class.getName());
    public static final String TAG_WORLD_LIGHT_SOURCE_PERSISTOR = "varlight:persistor";

    private static final Map<UUID, Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>>> LIGHT_SOURCES = new HashMap<>();

    public static Stream<LightSourcePersistor> getAllPersistors(VarLightPlugin plugin) {
        return Bukkit.getWorlds().stream().map(w -> getPersistor(plugin, w));
    }

    public static LightSourcePersistor getPersistor(VarLightPlugin plugin, World world) {
        return (LightSourcePersistor) world.getMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR).stream().filter(m -> m.getOwningPlugin().equals(plugin)).findFirst().orElseGet(() -> {

            FixedMetadataValue fixedMetadataValue = new FixedMetadataValue(plugin, new LightSourcePersistor(plugin, world));
            world.setMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR, fixedMetadataValue);

            return fixedMetadataValue;

        }).value();
    }

    private final VarLightPlugin plugin;
    private final World world;

    private LightSourcePersistor(VarLightPlugin plugin, World world) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);

        LOGGER.info(String.format("Created a new Lightsource Persistor for world \"%s\"", world.getName()));

        this.plugin = plugin;
        this.world = world;

        LIGHT_SOURCES.put(world.getUID(), new HashMap<>());
    }

    public VarLightPlugin getPlugin() {
        return plugin;
    }

    public World getWorld() {
        return world;
    }

    public int getEmittingLightLevel(Location location) {
        return getPersistentLightSource(location).map(PersistentLightSource::getEmittingLight).orElse((byte) 0);
    }

    public Optional<PersistentLightSource> getPersistentLightSource(Location location) {
        return getPersistentLightSource(new IntPosition(location));
    }

    public Optional<PersistentLightSource> getPersistentLightSource(int x, int y, int z) {
        return getPersistentLightSource(new IntPosition(x, y, z));
    }

    public Optional<PersistentLightSource> getPersistentLightSource(IntPosition intPosition) {
        synchronized (world) {
            return Optional.ofNullable(getRegionMap(new RegionCoordinates(intPosition)).get(intPosition));
        }
    }

    public PersistentLightSource createPersistentLightSource(IntPosition intPosition, int emittingLight) {
        synchronized (world) {
            PersistentLightSource persistentLightSource = new PersistentLightSource(plugin, world, intPosition, emittingLight);
            PersistentLightSource previous = getRegionMap(new RegionCoordinates(intPosition)).put(intPosition, persistentLightSource);

            if (previous != null) {
                previous.invalidate();
            }

            return persistentLightSource;
        }
    }

    public PersistentLightSource getOrCreatePersistentLightSource(IntPosition position) {
        return getPersistentLightSource(position).orElseGet(() -> createPersistentLightSource(position, 0));
    }

    public void save() {
        synchronized (world) {
            Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> worldMap = getWorldMap();
            Gson gson = new Gson();

            for (Map.Entry<RegionCoordinates, Map<IntPosition, PersistentLightSource>> entry : worldMap.entrySet()) {
                File saveFile = getSaveFile(entry.getKey());

                int written = 0;

                try (JsonWriter jsonWriter = new JsonWriter(new FileWriter(saveFile))) {
                    jsonWriter.beginArray();

                    for (PersistentLightSource persistentLightSource : entry.getValue().values()) {
                        if (persistentLightSource.writeIfValid(gson, jsonWriter)) {
                            written++;
                        }
                    }

                    jsonWriter.endArray();
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                if (written == 0) {
                    saveFile.delete();
                }
            }

            LOGGER.info(String.format("All Custom Light Sources persisted for World \"%s\"", world.getName()));
        }

    }

    private Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> getWorldMap() {
        return LIGHT_SOURCES.get(world.getUID());
    }

    private Map<IntPosition, PersistentLightSource> getRegionMap(RegionCoordinates regionCoordinates) {
        Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> worldMap = getWorldMap();

        if (! worldMap.containsKey(regionCoordinates)) {

            File regionFile = getSaveFile(regionCoordinates);

            if (! regionFile.exists()) {
                worldMap.put(regionCoordinates, new HashMap<>());
            } else {
                worldMap.put(regionCoordinates, loadRegionMap(regionFile));
            }
        }

        return worldMap.get(regionCoordinates);
    }

    private File getSaveDirectory() {
        File varlightDir = new File(world.getWorldFolder(), "varlight");

        if (! varlightDir.exists()) {
            if (! varlightDir.mkdir()) {
                throw new LightPersistFailedException();
            }
        }

        return varlightDir;
    }

    private List<PersistentLightSource> getValidLightSources() {
        synchronized (world) {
            Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> worldMap = getWorldMap();
            List<PersistentLightSource> valid = new ArrayList<>();

            for (Map.Entry<RegionCoordinates, Map<IntPosition, PersistentLightSource>> entry : worldMap.entrySet()) {
                for (PersistentLightSource persistentLightSource : entry.getValue().values()) {
                    if (persistentLightSource.checkValidStatus()) {
                        valid.add(persistentLightSource);
                    }
                }
            }

            return valid;
        }
    }

    private File getSaveFile(RegionCoordinates regionCoordinates) {
        return new File(getSaveDirectory(), String.format("r.%d.%d.json", regionCoordinates.getRegionX(), regionCoordinates.getRegionZ()));
    }

    private Map<IntPosition, PersistentLightSource> loadRegionMap(File file) {
        Map<IntPosition, PersistentLightSource> regionMap = new HashMap<>();
        Gson gson = new Gson();

        try (JsonReader jsonReader = new JsonReader(new FileReader(file))) {
            jsonReader.beginArray();

            while (true) {
                if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                    break;
                }

                PersistentLightSource persistentLightSource = PersistentLightSource.read(gson, jsonReader);
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
