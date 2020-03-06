package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.ChunkCoords;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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

    @NotNull
    @Override
    public String materialToKey(Material material) {
        throw new AbstractMethodError();
    }

    @NotNull
    @Override
    public String getLocalizedBlockName(Material material) {
        throw new AbstractMethodError();
    }

    @NotNull
    @Override
    public Collection<String> getTypes(MaterialType type) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isIllegalLightUpdateItem(Material material) {
        throw new AbstractMethodError();
    }

    @Override
    public void updateLight(@NotNull Location at, int lightLevel) {
        throw new AbstractMethodError();
    }

    @Override
    public void updateLight(World world, ChunkCoords chunkCoords) {
        throw new AbstractMethodError();
    }

    @Override
    public void updateBlocks(Chunk chunk) {
        throw new AbstractMethodError();
    }

    @Override
    public boolean isIllegalBlock(@NotNull Material block) {
        throw new AbstractMethodError();
    }

    @NotNull
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
    public @NotNull File getRegionRoot(World world) {
        throw new AbstractMethodError();
    }

}
