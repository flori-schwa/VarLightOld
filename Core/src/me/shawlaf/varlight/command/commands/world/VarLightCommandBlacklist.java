package me.shawlaf.varlight.command.commands.world;

import me.shawlaf.varlight.VarLightConfiguration;
import me.shawlaf.varlight.VarLightPlugin;

public class VarLightCommandBlacklist extends VarLightCommandWorld {

    public VarLightCommandBlacklist(VarLightPlugin plugin) {
        super(plugin, VarLightConfiguration.WorldListType.BLACKLIST);
    }

}
