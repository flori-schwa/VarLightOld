package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import me.shawlaf.varlight.persistence.PersistentLightSource;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.command.VarLightCommand.SUCCESS;

public class VarLightCommandMigrate extends VarLightSubCommand {

    public VarLightCommandMigrate(VarLightPlugin plugin) {
        super(plugin, "migrate");
    }

    @NotNull
    @Override
    public String getSyntax() {
        return "";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Migrates all light sources after upgrading your server to 1.14.2+";
    }


    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.migrate";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.executes(
                context -> {
                    CommandSender sender = context.getSource();

                    if (!plugin.getNmsAdapter().getMinecraftVersion().newerOrEquals(VarLightPlugin.MC1_14_2)) {
                        failure(this, sender, "You may only migrate AFTER Minecraft 1.14.2!");

                        return FAILURE;
                    }

                    int totalMigrated = 0, totalSkipped = 0;

                    successBroadcast(this, sender, "Starting migration...");

                    for (WorldLightSourceManager manager : plugin.getAllManagers()) {
                        int migrated = 0, skipped = 0;

                        successBroadcast(this, sender, String.format("Migrating \"%s\"", manager.getWorld().getName()));

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

                        successBroadcast(this, sender, String.format("Migrated Light sources in world \"%s\" (migrated: %d, skipped: %d)", manager.getWorld().getName(), migrated, skipped));

                        totalMigrated += migrated;
                        totalSkipped += skipped;
                    }

                    successBroadcast(this, sender, String.format("All Light sources migrated (total migrated: %d, skipped: %d)", totalMigrated, totalSkipped));

                    return SUCCESS;
                }
        );

        return node;
    }
}
