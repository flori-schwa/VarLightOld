package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public interface INmsAdapter {

    default void onLoad() {

    }

    default void onEnable() {

    }

    default void onDisable() {

    }

    default void onWorldEnable(@NotNull World world) {

    }

    default void handleBlockUpdate(BlockEvent blockEvent) {

    }

    default String getForMinecraftVersion() {
        return this.getClass().getAnnotation(ForMinecraft.class).version();
    }

    @Nullable Material keyToType(String namespacedKey, MaterialType type);

    boolean isCorrectTool(Material block, Material tool);

    String materialToKey(Material material);

    String getLocalizedBlockName(Material material);

    Collection<String> getTypes(MaterialType type);

    boolean isIllegalLightUpdateItem(Material material);

    boolean isBlockTransparent(@NotNull Block block);

    void updateBlockLight(@NotNull Location at, int lightLevel);

    int getEmittingLightLevel(@NotNull Block block);

    void sendChunkUpdates(@NotNull Chunk chunk, int mask);

    default void sendChunkUpdates(@NotNull Chunk chunk) {
        sendChunkUpdates(chunk, (1 << 16) - 1);
    }

    boolean isIllegalBlock(@NotNull Block block);

    void sendActionBarMessage(Player player, String message);

    ItemStack getVarLightDebugStick();

    ItemStack makeGlowingStack(ItemStack base, int lightLevel);

    int getGlowingValue(ItemStack glowingStack);

    @Nullable
    Block getTargetBlockExact(Player player, int maxDistance);

    @NotNull
    String getNumericMinecraftVersion();

    default boolean isVarLightDebugStick(ItemStack itemStack) {
        return itemStack.equals(getVarLightDebugStick());
    }

    @NotNull
    default NumericMajorMinorVersion getMinecraftVersion() {
        return new NumericMajorMinorVersion(getNumericMinecraftVersion());
    }

    default int getChunkBitMask(@NotNull Location location) {
        Objects.requireNonNull(location);

        return getChunkBitMask(location.getBlockY() / 16);
    }

    default int getChunkBitMask(int sectionY) {
        int mask = 1 << sectionY;

        if (sectionY == 0) {
            return mask | 2;
        }

        if (sectionY == 15) {
            return mask | 0x4000;
        }

        return mask | (1 << (sectionY - 1)) | (1 << (sectionY + 1));
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

        int chunkX = location.getChunkX();
        int chunkZ = location.getChunkZ();

        List<Chunk> chunksToUpdate = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;

                if (!world.isChunkLoaded(x, z)) {
                    continue;
                }

                chunksToUpdate.add(world.getChunkAt(x, z));
            }
        }
        return chunksToUpdate;
    }

    default boolean areKeysEqual(String keyA, String keyB) {
        String aNs = "minecraft", aK, bNs = "minecraft", bK;

        int i;

        if ((i = keyA.indexOf(':')) > 0) { // Key is namespaced
            aNs = keyA.substring(0, i);
            aK = keyA.substring(i + 1);
        } else {
            aK = keyA;
        }

        if ((i = keyB.indexOf(':')) > 0) { // Key is namespaced
            bNs = keyB.substring(0, i);
            bK = keyB.substring(i + 1);
        } else {
            bK = keyB;
        }

        return aNs.equals(bNs) && aK.equals(bK);
    }
}
