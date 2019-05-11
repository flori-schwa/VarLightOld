package me.florian.varlight;

import me.florian.varlight.nms.persistence.LightSourcePersistor;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class LightUpdateEvent extends BlockEvent implements Cancellable {

    public static final HandlerList HANDLERS = new HandlerList();

    public LightUpdateEvent(VarLightPlugin varLightPlugin, Block block, int mod) {
        this(block, block.getLightFromBlocks(), LightSourcePersistor.getPersistor(varLightPlugin, block.getWorld()).getEmittingLightLevel(block.getLocation()) + mod);
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

        this.fromLight = fromLight & 0xF;
        this.toLight = toLight & 0xF;
    }

    public int getFromLight() {
        return fromLight;
    }

    public int getToLight() {
        return toLight;
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
