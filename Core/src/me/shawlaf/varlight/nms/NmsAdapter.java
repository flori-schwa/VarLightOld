package me.shawlaf.varlight.nms;

import me.shawlaf.varlight.VarLightPlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@ForMinecraft(version = "UNDEFINED")
public class NmsAdapter implements INmsAdapter {

    public NmsAdapter(VarLightPlugin plugin) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isBlockTransparent(@NotNull Block block) {
        throw new AbstractMethodError();
    }

    @Override
    public void updateBlockLight(@NotNull Location at, int lightLevel) {
        throw new AbstractMethodError();
    }

    @Override
    public int getEmittingLightLevel(@NotNull Block block) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendChunkUpdates(@NotNull Chunk chunk, int mask) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isIllegalBlock(@NotNull Block block) {
        throw new AbstractMethodError();
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        throw new AbstractMethodError();
    }

    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        throw new AbstractMethodError();
    }

    @Override
    public Block getTargetBlockExact(Player player, int maxDistance) {
        throw new AbstractMethodError();
    }
}
