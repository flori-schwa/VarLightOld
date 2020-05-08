package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.ChunkCoords;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

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
    public @NotNull String getLocalizedBlockName(Material material) {
        throw new AbstractMethodError();
    }

    @Override
    public @NotNull Collection<String> getTypes(MaterialType type) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isIllegalLightUpdateItem(Material material) {
        throw new AbstractMethodError();
    }

    @Override
    public void updateBlocksAndChunk(@NotNull Location at) {
        throw new AbstractMethodError();
    }

    @Override
    public void updateChunk(World world, ChunkCoords chunkCoords) {
        throw new AbstractMethodError();
    }

    @Override
    public CompletableFuture<Void> updateBlocks(World world, ChunkCoords chunkCoords) {
        throw new AbstractMethodError();
    }

    @Override
    public CompletableFuture<Void> updateBlock(Location at) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isIllegalBlock(@NotNull Material material) {
        throw new AbstractMethodError();
    }

    @Override
    public @NotNull ItemStack getVarLightDebugStick() {
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

    @Override
    public @NotNull File getRegionRoot(World world) {
        throw new AbstractMethodError();
    }

    @Override
    public String getDefaultLevelName() {
        throw new AbstractMethodError();
    }
}
