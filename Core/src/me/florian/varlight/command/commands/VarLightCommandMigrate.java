package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightCommand;
import me.florian.varlight.command.VarLightSubCommand;
import me.florian.varlight.command.exception.VarLightCommandException;
import me.florian.varlight.persistence.LightSourcePersistor;
import me.florian.varlight.persistence.PersistentLightSource;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

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

        class IntContainer {
            int i = 0;
        }

        IntContainer totalMigrated = new IntContainer(), totalSkipped = new IntContainer();

        VarLightCommand.broadcastResult(sender, "Starting migration...", node);

        LightSourcePersistor.getAllPersistors(plugin).forEach((p) -> {

            IntContainer migrated = new IntContainer(), skipped = new IntContainer();

            VarLightCommand.broadcastResult(sender, String.format("Migrating \"%s\"", p.getWorld().getName()), node);

            p.getAllLightSources().filter(PersistentLightSource::needsMigration).forEach(lightSource -> {
                if (!lightSource.getPosition().isChunkLoaded(lightSource.getWorld())) {
                    if (lightSource.getPosition().loadChunk(lightSource.getWorld(), false)) {
                        lightSource.update();
                        migrated.i++;
                    } else {
                        skipped.i++;
                    }
                } else {
                    lightSource.update();
                    migrated.i++;
                }
            });

            VarLightCommand.broadcastResult(sender, String.format("Migrated Light sources in world \"%s\" (migrated: %d, skipped: %d)", p.getWorld().getName(), migrated.i, skipped.i), node);

            totalMigrated.i += migrated.i;
            totalSkipped.i += skipped.i;
        });

        VarLightCommand.broadcastResult(sender, String.format("All Light sources migrated (total migrated: %d, skipped: %d)", totalMigrated.i, totalSkipped.i), node);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, ArgumentIterator args) {
        return new ArrayList<>();
    }
}
