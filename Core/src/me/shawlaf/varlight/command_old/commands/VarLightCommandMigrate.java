package me.shawlaf.varlight.command_old.commands;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command_old.ArgumentIterator;
import me.shawlaf.varlight.command_old.VarLightCommand;
import me.shawlaf.varlight.command_old.VarLightSubCommand;
import me.shawlaf.varlight.command_old.exception.VarLightCommandException;
import me.shawlaf.varlight.persistence.PersistentLightSource;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import org.bukkit.command.CommandSender;

@Deprecated
public class VarLightCommandMigrate extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandMigrate(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public String getSyntax() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Migrates all light sources after upgrading your server to 1.14.2+";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        final String node = "varlight.admin";

        VarLightCommand.assertPermission(sender, node);

        if (!plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2)) {
            throw new VarLightCommandException("You may only migrate AFTER Minecraft 1.14.2!");
        }

        int totalMigrated = 0, totalSkipped = 0;

        VarLightCommand.broadcastResult(sender, "Starting migration...", node);

        for (WorldLightSourceManager manager : plugin.getAllManagers()) {
            int migrated = 0, skipped = 0;

            VarLightCommand.broadcastResult(sender, String.format("Migrating \"%s\"", manager.getWorld().getName()), node);

            for (PersistentLightSource lightSource : manager.getAllLightSources()) {

                if (lightSource.needsMigration()) {
                    if (!lightSource.getPosition().isChunkLoaded(lightSource.getWorld())) {
                        if (lightSource.getPosition().loadChunk(lightSource.getWorld(), false)) {
                            lightSource.update();
                            migrated++;
                        } else {
                            skipped++;
                        }
                    } else {
                        lightSource.update();
                        migrated++;
                    }
                }

            }

            VarLightCommand.broadcastResult(sender, String.format("Migrated Light sources in world \"%s\" (migrated: %d, skipped: %d)", manager.getWorld().getName(), migrated, skipped), node);

            totalMigrated += migrated;
            totalSkipped += skipped;
        }

        VarLightCommand.broadcastResult(sender, String.format("All Light sources migrated (total migrated: %d, skipped: %d)", totalMigrated, totalSkipped), node);

        return true;
    }
}
