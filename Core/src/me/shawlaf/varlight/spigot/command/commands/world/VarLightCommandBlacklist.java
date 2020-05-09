package me.shawlaf.varlight.spigot.command.commands.world;

import me.shawlaf.varlight.spigot.VarLightConfiguration;
import me.shawlaf.varlight.spigot.command.VarLightCommand;

@Deprecated
public class VarLightCommandBlacklist extends VarLightCommandWorld {

    public VarLightCommandBlacklist(VarLightCommand command) {
        super(command, VarLightConfiguration.WorldListType.BLACKLIST);
    }

}
