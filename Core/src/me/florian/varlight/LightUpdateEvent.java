package me.florian.varlight;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class LightUpdateEvent extends BlockEvent implements Cancellable {

    public static final HandlerList HANDLERS = new HandlerList();

    public LightUpdateEvent(Block block, int mod) {
        this(block, block.getLightFromBlocks(), mod > 0 ? Math.min(block.getLightFromBlocks() + mod, 15) : Math.max(block.getLightFromBlocks() + mod, 0));
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    private final int fromLight;
    private int toLight;
    private boolean cancelled = false;

    public LightUpdateEvent(Block theBlock, int fromLight, int toLight) {
        super(theBlock);

        if (fromLight < 0 || fromLight > 15 || toLight < 0 || toLight > 15) {
            throw new IllegalArgumentException("Light values must be in the range of 0 - 15");
        }

        this.fromLight = fromLight;
        this.toLight = toLight;
    }

    public int getFromLight() {
        return fromLight;
    }

    public int getToLight() {
        return toLight;
    }

    public void setToLight(int toLight) {
        if (toLight < 0 || toLight > 15) {
            throw new IllegalArgumentException("Light values must be in the range of 0 - 15");
        }

        this.toLight = toLight;
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
