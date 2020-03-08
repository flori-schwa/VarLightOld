package me.shawlaf.varlight.spigot.persistence.migrate;

import me.shawlaf.varlight.persistence.migrate.LightDatabaseMigrator;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.World;

import java.io.File;

public class LightDatabaseMigratorSpigot extends LightDatabaseMigrator<World> {

    private final VarLightPlugin plugin;

    public LightDatabaseMigratorSpigot(VarLightPlugin plugin) {
        super(plugin.getLogger());

        this.plugin = plugin;
    }

    @Override
    protected File getVarLightSaveDirectory(World world) {
        return plugin.getNmsAdapter().getVarLightSaveDirectory(world);
    }

    @Override
    protected String getName(World world) {
        return world.getName();
    }
}
