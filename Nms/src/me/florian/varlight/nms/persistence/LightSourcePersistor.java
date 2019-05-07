package me.florian.varlight.nms.persistence;

import com.google.gson.stream.JsonWriter;
import me.florian.varlight.IntPosition;
import me.florian.varlight.RegionCoordinates;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LightSourcePersistor {

    public static final String TAG_WORLD_LIGHT_SOURCE_PERSISTOR = "varlight:persistor";

    private static final Map<UUID, Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>>> LIGHT_SOURCES = new HashMap<>();


    public static LightSourcePersistor getPersistor(Plugin plugin, World world) {
        return (LightSourcePersistor) world.getMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR).stream().filter(m -> m.getOwningPlugin().equals(plugin)).findFirst().orElseGet(() -> {

            FixedMetadataValue fixedMetadataValue = new FixedMetadataValue(plugin, new LightSourcePersistor(world));
            world.setMetadata(TAG_WORLD_LIGHT_SOURCE_PERSISTOR, fixedMetadataValue);

            return fixedMetadataValue;

        }).value();
    }

    private final World world;

    private LightSourcePersistor(World world) {
        Objects.requireNonNull(world);

        this.world = world;

        LIGHT_SOURCES.put(world.getUID(), new HashMap<>());
    }

    private Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> getWorldMap() {
        return LIGHT_SOURCES.get(world.getUID());
    }

    private Map<IntPosition, PersistentLightSource> getRegionMap(RegionCoordinates regionCoordinates) {
        Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> worldMap = getWorldMap();

        if (! worldMap.containsKey(regionCoordinates)) {
            worldMap.put(regionCoordinates, new HashMap<>());
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

    private File getSaveFile(RegionCoordinates regionCoordinates) {
        return new File(getSaveDirectory(), String.format("r.%d.%d.json", regionCoordinates.getRegionX(), regionCoordinates.getRegionZ()));
    }

    public boolean hasLightSource(Location location) {
        return hasLightSource(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean hasLightSource(int x, int y, int z) {
        return hasLightSource(new IntPosition(x, y, z));
    }

    public boolean hasLightSource(IntPosition intPosition) {
        synchronized (world) {
            Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> worldMap = getWorldMap();
            RegionCoordinates regionCoordinates = new RegionCoordinates(intPosition);

            if (! worldMap.containsKey(regionCoordinates)) {
                return false;
            }

            return worldMap.get(regionCoordinates).containsKey(intPosition);
        }
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
            PersistentLightSource persistentLightSource = new PersistentLightSource(world, intPosition, emittingLight);

            Optional.ofNullable(getRegionMap(new RegionCoordinates(intPosition)).put(intPosition, persistentLightSource))
                    .ifPresent(PersistentLightSource::invalidate);

            return persistentLightSource;
        }
    }

    public PersistentLightSource getOrCreatePersistentLightSource(IntPosition position, int emittingLight) {
        return getPersistentLightSource(position).orElse(createPersistentLightSource(position, emittingLight));
    }

    public void save(Plugin plugin) {
        synchronized (world) {
            Map<RegionCoordinates, Map<IntPosition, PersistentLightSource>> worldMap = getWorldMap();

            for (Map.Entry<RegionCoordinates, Map<IntPosition, PersistentLightSource>> entry : worldMap.entrySet()) {
                File saveFile = getSaveFile(entry.getKey());

                try (JsonWriter jsonWriter = new JsonWriter(new FileWriter(saveFile))) {
                    jsonWriter.beginArray();                                                            // [


                    for (PersistentLightSource persistentLightSource : entry.getValue().values()) {
                        if (! persistentLightSource.isStillValid()) {
                            continue;
                        }

                        persistentLightSource.write(jsonWriter);                                        //      { position: { ... }, ... }, ...
                    }

                    jsonWriter.endArray();                                                              //  ]
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                plugin.getLogger().info(String.format("Saved %s", saveFile.getName()));
            }

            plugin.getLogger().info("All Custom Light Sources persisted");
        }

    }

}
