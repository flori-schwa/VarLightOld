package me.florian.varlight.nms.persistence;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.florian.varlight.IntPosition;
import me.florian.varlight.VarLightPlugin;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

public class PersistentLightSource {

    private transient boolean valid = true;
    private transient final World world;
    private transient VarLightPlugin plugin;

    private final IntPosition position;
    private final Material type;
    private byte emittingLight;

    PersistentLightSource(VarLightPlugin plugin, World world, IntPosition position, int emittingLight) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);
        Objects.requireNonNull(position);

        this.plugin = plugin;
        this.world = world;
        this.position = position;
        this.type = position.toLocation(world).getBlock().getType();
        this.emittingLight = (byte) (emittingLight & 0xF);

        System.out.println("new created");
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

    public byte getEmittingLight() {

        if (! checkValidStatus()) {
            invalidate();
            return 0;
        }

        return emittingLight;
    }

    public void setEmittingLight(int lightLevel) {
        this.emittingLight = (byte) (lightLevel & 0xF);
    }

    public void invalidate() {
        System.out.println("invalidated");
        valid = false;
    }

    public boolean checkValidStatus() {
        if (! valid) {
            return false;
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

    static PersistentLightSource read(Gson gson, JsonReader jsonReader) {
        return gson.fromJson(jsonReader, PersistentLightSource.class);
    }

    boolean writeIfValid(Gson gson, JsonWriter jsonWriter) {
        if (checkValidStatus()) {
            gson.toJson(this, PersistentLightSource.class, jsonWriter);
            return true;
        }

        return false;
    }
}
