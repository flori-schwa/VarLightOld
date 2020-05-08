package me.shawlaf.varlight.spigot.event;

import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public class LightUpdateEvent extends Event implements Cancellable {

    public static final HandlerList HANDLERS = new HandlerList();
    private final int fromLight;
    private final World world;
    private final IntPosition position;
    private final CommandSender source;

    private int toLight;
    private boolean cancelled = false;

    public LightUpdateEvent(CommandSender source, Block theBlock, int fromLight, int toLight) {
        this(source, theBlock, fromLight, toLight, false);
    }

    public LightUpdateEvent(CommandSender source, Block theBlock, int fromLight, int toLight, boolean async) {
        this(source, theBlock.getWorld(), toIntPosition(theBlock), fromLight, toLight, async);
    }

    public LightUpdateEvent(CommandSender source, World world, IntPosition position, int fromLight, int toLight) {
        this(source, world, position, fromLight, toLight, false);
    }

    public LightUpdateEvent(CommandSender source, World world, IntPosition position, int fromLight, int toLight, boolean async) {
        super(async);

        this.world = world;
        this.position = position;
        this.source = source;
        this.fromLight = fromLight & 0xF;
        this.toLight = toLight & 0xF;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public World getWorld() {
        return world;
    }

    public IntPosition getPosition() {
        return position;
    }

    public Location toLocation() {
        return IntPositionExtension.toLocation(position, world);
    }

    public CommandSender getSource() {
        return source;
    }

    public int getFromLight() {
        return fromLight & 0xF;
    }

    public int getToLight() {
        return toLight & 0xF;
    }

    public void setToLight(int toLight) {
        this.toLight = toLight & 0xF;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
