package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@ForMinecraft(version = "UNDEFINED")
public class NmsAdapter implements INmsAdapter {

    public NmsAdapter(VarLightPlugin plugin) {
        throw new AbstractMethodError();
    }

    @Override
    public @Nullable Material keyToType(String namespacedKey, MaterialType type) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isCorrectTool(Material block, Material tool) {
        throw new AbstractMethodError();
    }

    @Override
    public String materialToKey(Material material) {
        throw new AbstractMethodError();
    }

    @Override
    public String getLocalizedBlockName(Material material) {
        throw new AbstractMethodError();
    }

    @Override
    public Collection<String> getTypes(MaterialType type) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isIllegalLightUpdateItem(Material material) {
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

    @Override
    public ItemStack getVarLightDebugStick() {
        throw new AbstractMethodError();
    }

    @Override
    public ItemStack makeGlowingStack(ItemStack base, int lightLevel) {
        throw new AbstractMethodError();
    }

    @Override
    public int getGlowingValue(ItemStack glowingStack) {
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
