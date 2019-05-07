package me.florian.varlight.nms.persistence;

import com.google.gson.stream.JsonWriter;
import me.florian.varlight.IntPosition;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.IOException;
import java.util.Objects;

public class PersistentLightSource {

    private boolean valid = true;

    private final World world;
    private final IntPosition position;
    private final Material type;
    private byte emittingLight;

    PersistentLightSource(World world, IntPosition position, int emittingLight) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(position);

        this.world = world;
        this.position = position;
        this.type = position.toLocation(world).getBlock().getType();
        this.emittingLight = (byte) (emittingLight & 0xF);
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

        if (! isStillValid()) {
            invalidate();
            return 0;
        }

        return emittingLight;
    }

    public void setEmittingLight(int lightLevel) {
        this.emittingLight = (byte) (lightLevel & 0xF);
    }

    public void invalidate() {
        valid = false;
    }

    public boolean isStillValid() {
        if (! valid) {
            return false;
        }

        Block block = position.toBlock(world);

        if (block.getType() != type) {
            return false;
        }

        return block.getLightFromBlocks() >= emittingLight;
    }

    public void write(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();               //  {

        jsonWriter.name("position");
        jsonWriter.beginObject();               //      position: {

        jsonWriter.name("x");
        jsonWriter.value(position.getX());      //          x: x,

        jsonWriter.name("y");
        jsonWriter.value(position.getY());      //          y: y,

        jsonWriter.name("z");
        jsonWriter.value(position.getZ());      //          z: z,

        jsonWriter.endObject();                 //      },

        jsonWriter.name("type");
        jsonWriter.value(type.name());          //      type: type,

        jsonWriter.name("emittingLight");
        jsonWriter.value(emittingLight);        //      emittingLight: emittingLight

        jsonWriter.endObject();                 //  }
    }
}
