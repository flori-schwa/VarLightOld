package me.shawlaf.varlight.spigot.command.commands.world;

import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.VarLightPlugin;

public class VarLightCommandWhitelist extends VarLightCommandWorld {

    public VarLightCommandWhitelist(VarLightPlugin plugin) {
        super(plugin, VarLightConfiguration.WorldListType.WHITELIST);
    }

}
