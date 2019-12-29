package me.shawlaf.varlight.persistence;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.ChatColor;
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
    public String getType() {
        return plugin.getNmsAdapter().materialToKey(type);
    }

    @Override
    public int getCustomLuminance() {
        return emittingLight & 0xF;
    }

    public int getEmittingLight() {

        if (isInvalid()) {
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

    public boolean needsMigration() {
        return plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2) && !isMigrated();
    }

    public void migrate() {
        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            return;
        }

        plugin.getNmsAdapter().updateBlockLight(position.toLocation(world), emittingLight);
        manager.createPersistentLightSource(position, emittingLight);
        migrated = true;
    }

    public void update() {
        if (needsMigration() && world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            migrate();
        }
    }

    public boolean isInvalid() {
        if (!world.isChunkLoaded(position.getChunkX(), position.getChunkZ())) {
            return false; // Assume valid
        }

        if (plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2) && !migrated) {
            return false; // Assume valid
        }

        Block block = position.toBlock(world);

        if (block.getType() != type) {
            return true;
        }

        if (plugin.getNmsAdapter().isIllegalBlock(block)) {
            return true;
        }

        return block.getLightFromBlocks() < emittingLight;
    }

    public String toCompactString(boolean colored) {
        boolean valid = !isInvalid();

        if (colored) {
            return String.format("%s%s %s%s%s %s [%d, %d, %d] = %d",
                    valid ? ChatColor.GREEN : ChatColor.RED,
                    valid ? "VALID" : "INVALID",
                    migrated ? ChatColor.GREEN : ChatColor.RED,
                    migrated ? "MIGRATED" : "NOT MIGRATED",
                    valid ? ChatColor.GREEN : ChatColor.RED,
                    plugin.getNmsAdapter().materialToKey(type),
                    position.x,
                    position.y,
                    position.z,
                    emittingLight
            );
        } else {
            return String.format("%s %s %s [%d, %d, %d] = %d",
                    valid ? "VALID" : "INVALID",
                    migrated ? "MIGRATED" : "NOT MIGRATED",
                    plugin.getNmsAdapter().materialToKey(type),
                    position.x,
                    position.y,
                    position.z,
                    emittingLight
            );
        }
    }

    @Override
    public String toString() {
        return "PersistentLightSource{" +
                "position=" + position +
                ", type=" + type +
                ", migrated=" + migrated +
                ", world=" + world +
                ", plugin=" + plugin +
                ", emittingLight=" + emittingLight +
                '}';
    }
}
