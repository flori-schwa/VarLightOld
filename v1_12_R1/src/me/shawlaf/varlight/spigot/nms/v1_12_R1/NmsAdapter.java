package me.shawlaf.varlight.spigot.nms.v1_12_R1;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.INmsAdapter;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.Item;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NmsAdapter implements INmsAdapter {

    private final VarLightPlugin plugin;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable Material keyToType(String namespacedKey, MaterialType type) {
        MinecraftKey key = new MinecraftKey(namespacedKey);

        switch (type) {
            case ITEM: {
                return CraftMagicNumbers.getMaterial(Item.REGISTRY.get(key));
            }

            case BLOCK: {
                return CraftMagicNumbers.getMaterial(Block.REGISTRY.get(key));
            }
        }

        return null;
    }

    @Override
    public @NotNull String getLocalizedBlockName(Material material) {
        return CraftMagicNumbers.getBlock(material).getName();
    }

    @Override
    public @NotNull Collection<String> getTypes(MaterialType type) {
        List<String> types = new ArrayList<>();

        switch (type) {
            case ITEM: {
                for (MinecraftKey key : Item.REGISTRY.keySet()) {
                    types.add(key.toString());
                    types.add(key.getKey());
                }

                return types;
            }

            case BLOCK: {
                for (MinecraftKey key : net.minecraft.server.v1_12_R1.Block.REGISTRY.keySet()) {
                    types.add(key.toString());
                    types.add(key.getKey());
                }

                return types;
            }
        }

        return types;
    }

    @Override
    public boolean isIllegalLightUpdateItem(Material material) {
        return material.isBlock() || !material.isItem();
    }

    @Override
    public boolean isIllegalBlock(@NotNull Material material) {
        return false;
    }

    @Override
    public @NotNull ItemStack getVarLightDebugStick() {
        return null;
    }

    @Override
    public ItemStack makeGlowingStack(ItemStack base, int lightLevel) {
        return null;
    }

    @Override
    public int getGlowingValue(ItemStack glowingStack) {
        return 0;
    }

    @Override
    public @NotNull File getRegionRoot(World world) {
        return null;
    }

    @Override
    public void setLight(World world, IntPosition position, int lightLevel) {

    }

    @Override
    public void setLight(Location location, int lightLevel) {

    }

    @Override
    public CompletableFuture<Void> setAndUpdateLight(World world, IntPosition position, int lightLevel) {
        return null;
    }

    @Override
    public CompletableFuture<Void> setAndUpdateLight(Location location, int lightLevel) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateLight(World world, IntPosition position) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateLight(Location location) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateLight(World world, Collection<IntPosition> positions) {
        return null;
    }

    @Override
    public CompletableFuture<Void> recalculateChunk(Chunk chunk) {
        return null;
    }

    @Override
    public CompletableFuture<Void> recalculateChunk(World world, ChunkCoords chunkCoords) {
        return null;
    }

    @Override
    public void sendLightUpdates(World world, ChunkCoords center) {

    }

    @Override
    public CompletableFuture<Void> updateChunk(World world, ChunkCoords chunkCoords) {
        return null;
    }
}
