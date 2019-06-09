package me.florian.varlight.persistence;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.util.IntPosition;
import me.florian.varlight.util.NumericMajorMinorVersion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

public class PersistentLightSource {

    public static final NumericMajorMinorVersion V1_14_2 = new NumericMajorMinorVersion("1.14.2");

    private transient World world;
    private transient VarLightPlugin plugin;

    private final IntPosition position;
    private final Material type;
    private int emittingLight;

    boolean migrated = false;

    PersistentLightSource(VarLightPlugin plugin, World world, IntPosition position, int emittingLight) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);
        Objects.requireNonNull(position);

        this.plugin = plugin;
        this.world = world;
        this.position = position;
        this.type = position.toLocation(world).getBlock().getType();
        this.emittingLight = (emittingLight & 0xF);
    }

    public World getWorld() {
        return world;
    }

    public IntPosition getPosition() {
        return position;
    }

    public Material getType() {
        return type;
    }

    public int getEmittingLight() {

        if (! isValid()) {
            return 0;
        }

        return emittingLight & 0xF;
    }

    public boolean needsMigration() {
        return plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(V1_14_2) && ! isMigrated();
    }

    public boolean isMigrated() {
        return migrated;
    }

    public void migrate() {
        plugin.getLightUpdater().setLight(position.toLocation(world), emittingLight);
        migrated = true;
    }

    public void update() {
        if (needsMigration() && world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            migrate();
        }
    }

    public void setEmittingLight(int lightLevel) {
        this.emittingLight = (lightLevel & 0xF);
    }

    public boolean isValid() {
        if (! world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            return true; // Assume valid
        }

        Block block = position.toBlock(world);

        if (block.getType() != type) {
            return false;
        }

        if (! plugin.getNmsAdapter().isValidBlock(block)) {
            return false;
        }

        return block.getLightFromBlocks() >= emittingLight;
    }

    void initialize(World world, VarLightPlugin plugin) {
        this.world = world;
        this.plugin = plugin;
    }

    static PersistentLightSource read(Gson gson, JsonReader jsonReader) {
        return gson.fromJson(jsonReader, PersistentLightSource.class);
    }

    void write(Gson gson, JsonWriter jsonWriter) {
        gson.toJson(this, PersistentLightSource.class, jsonWriter);
    }


}
