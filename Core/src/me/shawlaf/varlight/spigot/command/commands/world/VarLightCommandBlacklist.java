package me.shawlaf.varlight.spigot.command.commands.world;

import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.VarLightPlugin;

@Deprecated
public class VarLightCommandBlacklist extends VarLightCommandWorld {

    public VarLightCommandBlacklist(VarLightPlugin plugin) {
        super(plugin, VarLightConfiguration.WorldListType.BLACKLIST);
    }

}
