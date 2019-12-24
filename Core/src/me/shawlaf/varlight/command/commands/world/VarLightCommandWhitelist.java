package me.shawlaf.varlight.command.commands.world;

import me.shawlaf.varlight.VarLightConfiguration;
import me.shawlaf.varlight.VarLightPlugin;

public class VarLightCommandWhitelist extends VarLightCommandWorld {

    public VarLightCommandWhitelist(VarLightPlugin plugin) {
        super(plugin, VarLightConfiguration.WorldListType.WHITELIST);
    }

}
