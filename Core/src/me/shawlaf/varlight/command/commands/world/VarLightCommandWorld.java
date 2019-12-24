package me.shawlaf.varlight.command.commands.world;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shawlaf.varlight.VarLightConfiguration;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VarLightCommandWorld extends VarLightSubCommand {

    private final VarLightConfiguration.WorldListType worldListType;

    public VarLightCommandWorld(VarLightPlugin plugin, VarLightConfiguration.WorldListType listType) {
        super(plugin, listType.getConfigPath());

        this.worldListType = listType;
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " add/remove/list [world]";
    }

    @NotNull
    @Override
    public String getDescription() {
        return String.format("Add/Remove worlds from the %s or list all worlds on the %s", worldListType.getName(), worldListType.getName());
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.world";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {
        return node;
    }
}
