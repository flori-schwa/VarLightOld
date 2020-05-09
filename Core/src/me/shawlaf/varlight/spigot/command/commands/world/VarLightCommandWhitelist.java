package me.shawlaf.varlight.spigot.command.commands.world;

import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.command.VarLightCommand;

@Deprecated
public class VarLightCommandWhitelist extends VarLightCommandWorld {

    public VarLightCommandWhitelist(VarLightCommand command) {
        super(command, VarLightConfiguration.WorldListType.WHITELIST);
    }

}
