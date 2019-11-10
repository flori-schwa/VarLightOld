package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

public class PersistentLightSource implements ICustomLightSource {

    private final IntPosition position;
    private final Material type;
    boolean migrated = false;
    private transient World world;
    private transient VarLightPlugin plugin;
    private int emittingLight;

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

    PersistentLightSource(IntPosition position, Material type, boolean migrated, World world, VarLightPlugin plugin, int emittingLight) {
        Objects.requireNonNull(position);
        Objects.requireNonNull(type);
        Objects.requireNonNull(world);
        Objects.requireNonNull(plugin);

        this.position = position;
        this.type = type;
        this.migrated = migrated;
        this.world = world;
        this.plugin = plugin;
        this.emittingLight = emittingLight;
    }

    @Override
    public IntPosition getPosition() {
        return position;
    }

    @Override
    public Material getType() {
        return type;
    }

    @Override
    public int getEmittingLight() {

        if (!isValid()) {
            return 0;
        }

        return emittingLight & 0xF;
    }

    @Override
    public boolean isMigrated() {
        return migrated;
    }

    public World getWorld() {
        return world;
    }

    public void setEmittingLight(int lightLevel) {
        this.emittingLight = (lightLevel & 0xF);
    }

    public boolean needsMigration() {
        return plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2) && !isMigrated();
    }

    public void migrate() {
        plugin.getNmsAdapter().updateBlockLight(position.toLocation(world), emittingLight);
        migrated = true;
    }

    public void update() {
        if (needsMigration() && world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            migrate();
        }
    }

    public boolean isValid() {
        if (!world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            return true; // Assume valid
        }

        Block block = position.toBlock(world);

        if (block.getType() != type) {
            return false;
        }

        if (plugin.getNmsAdapter().isIllegalBlock(block)) {
            return false;
        }

        return block.getLightFromBlocks() >= emittingLight;
    }
}
