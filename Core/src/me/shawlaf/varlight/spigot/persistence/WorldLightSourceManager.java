package me.shawlaf.varlight.spigot.persistence;

import me.shawlaf.command.result.CommandResult;
import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.persistence.nls.NLSFile;
import me.shawlaf.varlight.persistence.nls.exception.PositionOutOfBoundsException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.RegionCoords;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.IntSupplier;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public class WorldLightSourceManager {

    private final Map<RegionCoords, NLSFile> worldMap = new HashMap<>();
    private final VarLightPlugin plugin;
    private final World world;

    public WorldLightSourceManager(VarLightPlugin plugin, World world) {
        Objects.requireNonNull(plugin);
        Objects.requireNonNull(world);

        this.plugin = plugin;
        this.world = world;

        synchronized (worldMap) {
            plugin.getNmsAdapter().getVarLightSaveDirectory(world); // Ensure the directory exists

            plugin.getDatabaseMigrator().runMigrations(world);
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

        int custom;

        try {
            custom = getNLSFile(position.toRegionCoords()).getCustomLuminance(position);
        } catch (PositionOutOfBoundsException e) {
            plugin.getDebugManager().logDebugAction(Bukkit.getConsoleSender(), e::getMessage);
            return def.getAsInt();
        }

        if (custom == 0) {
            return def.getAsInt();
        }

        return custom;
    }

    public boolean hasChunkCustomLightData(ChunkCoords chunkCoords) {
        return getNLSFile(chunkCoords.toRegionCoords()).hasChunkData(chunkCoords);
    }

    public void setCustomLuminance(Location location, int lightLevel) {
        try {
            setCustomLuminance(toIntPosition(location), lightLevel);
        } catch (PositionOutOfBoundsException e) {
            plugin.getDebugManager().logDebugAction(Bukkit.getConsoleSender(), e::getMessage);
        }
    }

    public void setCustomLuminance(IntPosition position, int lightLevel) {
        try {
            getNLSFile(position.toRegionCoords()).setCustomLuminance(position, lightLevel);
        } catch (PositionOutOfBoundsException e) {
            plugin.getDebugManager().logDebugAction(Bukkit.getConsoleSender(), e::getMessage);
        }
    }

    public void save(CommandSender commandSender, boolean log) {
        int modified = 0, deleted = 0;
        List<RegionCoords> regionsToUnload = new ArrayList<>();

        synchronized (worldMap) {
            for (NLSFile nlsFile : worldMap.values()) {

                try {
                    if (nlsFile.save()) {
                        ++modified;
                    }
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                List<ChunkCoords> affected = nlsFile.getAffectedChunks();

                if (affected.size() == 0) {
                    if (nlsFile.file.exists()) {
                        if (!nlsFile.file.delete()) {
                            throw new LightPersistFailedException("Could not delete file " + nlsFile.file.getAbsolutePath());
                        } else {
                            if (log) {
                                CommandResult.info(plugin.getCommand(), commandSender, String.format("Deleted File %s", nlsFile.file.getName()));
                            }

                            ++deleted;
                        }
                    }

                    regionsToUnload.add(nlsFile.getRegionCoords());
                    continue;
                }

                boolean anyLoaded = false;

                for (ChunkCoords chunkCoords : affected) {
                    if (world.isChunkLoaded(chunkCoords.x, chunkCoords.z)) {
                        anyLoaded = true;
                        break;
                    }
                }

                if (!anyLoaded) {
                    regionsToUnload.add(nlsFile.getRegionCoords());
                }
            }

            for (RegionCoords regionCoords : regionsToUnload) {
                worldMap.remove(regionCoords).unload();
            }
        }

        if (log) {
            commandSender.sendMessage(String.format("[VarLight] Light Sources persisted for World \"%s\", Files modified: %d, Files deleted: %d", world.getName(), modified, deleted));
        }
    }

    @NotNull
    public NLSFile getNLSFile(RegionCoords regionCoords) {
        synchronized (worldMap) {
            if (!worldMap.containsKey(regionCoords)) {
                File file = new File(plugin.getNmsAdapter().getVarLightSaveDirectory(world), String.format(NLSFile.FILE_NAME_FORMAT, regionCoords.x, regionCoords.z));
                NLSFile nlsFile;

                try {
                    if (file.exists()) {
                        nlsFile = NLSFile.existingFile(file, plugin.shouldDeflate());
                    } else {
                        nlsFile = NLSFile.newFile(file, regionCoords.x, regionCoords.z, plugin.shouldDeflate());
                    }
                } catch (IOException e) {
                    throw new LightPersistFailedException(e);
                }

                worldMap.put(regionCoords, nlsFile);
            }

            return worldMap.get(regionCoords);
        }
    }
}
