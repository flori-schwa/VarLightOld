package me.florian.varlight.nms;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@ForMinecraft(version = "UNDEFINED")
public class NmsAdapter implements INmsAdapter {

    public NmsAdapter() {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isBlockTransparent(Block block) {
        throw new AbstractMethodError();
    }

    @Override
    public void updateBlockLight(Location at, int lightLevel) {
        throw new AbstractMethodError();
    }

    @Override
    public int getEmittingLightLevel(Block block) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendChunkUpdates(Chunk chunk, int mask) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isValidBlock(Block block) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        throw new AbstractMethodError();
    }

    @Override
    public void setCooldown(Player player, Material material, int ticks) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean hasCooldown(Player player, Material material) {
        throw new AbstractMethodError();
    }

    @Override
    public String getNumericMinecraftVersion() {
        throw new AbstractMethodError();
    }
}
