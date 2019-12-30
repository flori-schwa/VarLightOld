package me.shawlaf.varlight.spigot.event;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

import java.util.Optional;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public class LightUpdateEvent extends BlockEvent implements Cancellable {

    public static final HandlerList HANDLERS = new HandlerList();
    private final int fromLight;
    private int toLight;
    private boolean cancelled = false;

    public LightUpdateEvent(VarLightPlugin varLightPlugin, Block block, int mod) {
        this(block, block.getLightFromBlocks(),
                Optional.ofNullable(varLightPlugin.getManager(block.getWorld()))
                        .map(w -> w.getCustomLuminance(toIntPosition(block), 0)).orElse(0) + mod);
    }

    public LightUpdateEvent(Block theBlock, int fromLight, int toLight) {
        super(theBlock);

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
