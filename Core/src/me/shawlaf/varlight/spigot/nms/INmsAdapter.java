package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.updater.ILightUpdater;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public interface INmsAdapter extends ILightUpdater {

    @Nullable Material keyToType(String namespacedKey, MaterialType type);

    @NotNull String getLocalizedBlockName(Material material);

    @NotNull Collection<String> getTypes(MaterialType type);

    boolean isIllegalLightUpdateItem(Material material);



//    /**
//     * Runs checkBlock on all blocks in the specified Chunk
//     *
//     * @param world The world containing the chunk
//     * @param chunkCoords The coordinates of the chunk
//     * @return
//     */
//    @Deprecated
//    CompletableFuture<Void> resetBlocks(World world, ChunkCoords chunkCoords);

//    /**
//     * Runs checkBlock on all specified blocks
//     *
//     * @param world The world containing the blocks
//     * @param positions The Block Positions to run checkBlock on
//     * @return
//     */
//    @Deprecated
//    CompletableFuture<Void> updateBlocks(World world, Collection<IntPosition> positions);

//    /**
//     * Runs checkBlock on the Block at the specified Location
//     *
//     * @param at The location of the block
//     * @return
//     */
//    @Deprecated
//    CompletableFuture<Void> updateBlock(Location at);

    boolean isIllegalBlock(@NotNull Material material);

    @NotNull ItemStack getVarLightDebugStick();

    ItemStack makeGlowingStack(ItemStack base, int lightLevel);

    int getGlowingValue(ItemStack glowingStack);

    @NotNull File getRegionRoot(World world);

    default void onLoad() {

    }

    default void onEnable() {

    }

    default void onDisable() {

    }

    default void enableVarLightInWorld(@NotNull World world) {

    }

    default NamespacedKey getKey(Material material) {
        return material.getKey();
    }

    default boolean isLegacy() {
        return false;
    }

    default String getForMinecraftVersion() {
        return this.getClass().getAnnotation(ForMinecraft.class).version();
    }

    default boolean isVarLightDebugStick(ItemStack itemStack) {
        return itemStack.equals(getVarLightDebugStick());
    }

    default boolean isIllegalBlock(@NotNull Block block) {
        return isIllegalBlock(block.getType());
    }

    @NotNull
    default List<Chunk> collectChunksToUpdate(@NotNull Location location) {
        Objects.requireNonNull(location);

        return collectChunksToUpdate(toIntPosition(location), location.getWorld());
    }

    @NotNull
    default List<Chunk> collectChunksToUpdate(@NotNull IntPosition location, @NotNull World world) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(world);

        return collectChunkPositionsToUpdate(location).stream().map(cc -> world.getChunkAt(cc.x, cc.z)).collect(Collectors.toList());
    }

    @NotNull
    default List<ChunkCoords> collectChunkPositionsToUpdate(@NotNull IntPosition center) {
        Objects.requireNonNull(center);

        return collectChunkPositionsToUpdate(center.toChunkCoords());
    }

    @NotNull
    default List<ChunkCoords> collectChunkPositionsToUpdate(@NotNull ChunkCoords center) {
        Objects.requireNonNull(center);

        int cx = center.x;
        int cz = center.z;

        List<ChunkCoords> list = new ArrayList<>(9);

        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                list.add(new ChunkCoords(cx + dx, cz + dz));
            }
        }

        return list;
    }

    default File getVarLightSaveDirectory(World world) {
        File varlightDir = new File(getRegionRoot(world), "varlight");

        if (!varlightDir.exists()) {
            if (!varlightDir.mkdirs()) {
                throw new LightPersistFailedException(String.format("Could not create Varlight directory \"%s\"for world \"%s\"", varlightDir.getAbsolutePath(), world.getName()));
            }
        }

        return varlightDir;
    }
}
