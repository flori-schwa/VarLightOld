package me.shawlaf.varlight.spigot;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.CommandUtils;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.shawlaf.varlight.spigot.event.LightUpdateEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardExtension implements Listener {

    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;

    public WorldGuardExtension() {
        this.worldGuard = WorldGuard.getInstance();
        this.worldGuardPlugin = WorldGuardPlugin.inst();
    }

    @EventHandler
    public void onLightSourceEdit(LightUpdateEvent lightUpdateEvent) {
        if (!(lightUpdateEvent.getSource() instanceof Player)) {
            return;
        }

        Player source = (Player) lightUpdateEvent.getSource();

        World weWorld = BukkitAdapter.adapt(lightUpdateEvent.getWorld());

        LocalPlayer wrappedPlayer = worldGuardPlugin.wrapPlayer(source);

        boolean canByPass = worldGuard.getPlatform().getSessionManager().hasBypass(wrappedPlayer, weWorld);

        if (!canByPass) {
            RegionQuery regionQuery = worldGuard.getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location weLocation = BukkitAdapter.adapt(lightUpdateEvent.toLocation());
            ApplicableRegionSet lightModifyAt = regionQuery.getApplicableRegions(weLocation);

            if (!lightModifyAt.testState(wrappedPlayer, Flags.BUILD)) {
                lightUpdateEvent.setCancelled(true);

                String message = lightModifyAt.queryValue(wrappedPlayer, Flags.DENY_MESSAGE);
                message = worldGuard.getPlatform().getMatcher().replaceMacros(wrappedPlayer, message);
                message = CommandUtils.replaceColorMacros(message);

                wrappedPlayer.printRaw(message.replace("%what%", "edit custom Light sources"));
            }
        }
    }

    public boolean canModifyAt(Player source, Location at) {
        World weWorld = BukkitAdapter.adapt(at.getWorld());

        LocalPlayer wrappedPlayer = worldGuardPlugin.wrapPlayer(source);

        boolean canByPass = worldGuard.getInstance().getPlatform().getSessionManager().hasBypass(wrappedPlayer, weWorld);

        if (canByPass) {
            return true;
        }

        RegionQuery regionQuery = worldGuard.getPlatform().getRegionContainer().createQuery();

        com.sk89q.worldedit.util.Location weLocation = BukkitAdapter.adapt(at);

        return regionQuery.testState(weLocation, wrappedPlayer, Flags.BUILD);
    }

}
