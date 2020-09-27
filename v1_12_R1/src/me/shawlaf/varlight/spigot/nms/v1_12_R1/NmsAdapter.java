package me.shawlaf.varlight.spigot.nms.v1_12_R1;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.INmsAdapter;
import me.shawlaf.varlight.spigot.nms.LightUpdateFailedException;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joor.Reflect;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

public class NmsAdapter implements INmsAdapter {

    private static final BlockFace[] CHECK_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private final VarLightPlugin plugin;
    private final ItemStack varlightDebugStick;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

        net.minecraft.server.v1_12_R1.ItemStack nmsStack = new net.minecraft.server.v1_12_R1.ItemStack(Items.STICK);

        nmsStack.addEnchantment(Enchantments.DURABILITY, 1);
        nmsStack.a("CustomType", new NBTTagString("varlight:debug_stick"));

        this.varlightDebugStick = CraftItemStack.asBukkitCopy(nmsStack);
        ItemMeta meta = varlightDebugStick.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "VarLight Debug Stick");
        varlightDebugStick.setItemMeta(meta);
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
        if (HardcodedBlockList.ALLOWED_BLOCKS.contains(material)) {
            return false;
        }

        if (plugin.getConfiguration().isAllowExperimentalBlocks()) {
            return !HardcodedBlockList.EXPERIMENTAL_BLOCKS.contains(material);
        }

        return true;
    }

    @Override
    public @NotNull ItemStack getVarLightDebugStick() {
        net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(varlightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.a("idLeast", new NBTTagLong(id.getLeastSignificantBits()));
        nmsStack.a("idMost", new NBTTagLong(id.getMostSignificantBits()));

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public ItemStack makeGlowingStack(ItemStack base, int lightLevel) {
        net.minecraft.server.v1_12_R1.ItemStack nmsStack = new net.minecraft.server.v1_12_R1.ItemStack(
                CraftMagicNumbers.getItem(base.getType()),
                base.getAmount()
        );

        lightLevel &= 0xF;

        nmsStack.a("varlight:glowing", new NBTTagInt(lightLevel));

        ItemStack stack = CraftItemStack.asBukkitCopy(nmsStack);

        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Glowing " + getLocalizedBlockName(stack.getType()));
        meta.setLore(Collections.singletonList(ChatColor.RESET + "Emitting Light: " + lightLevel));

        stack.setItemMeta(meta);

        return stack;
    }

    @Override
    public int getGlowingValue(ItemStack glowingStack) {
        net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(glowingStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null || !tag.hasKey("varlight:glowing")) {
            return -1;
        }

        return tag.getInt("varlight:glowing");
    }

    @Override
    public @NotNull File getRegionRoot(World world) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();

        ChunkProviderServer chunkProvider = nmsWorld.getChunkProviderServer();
        IChunkLoader chunkLoader = Reflect.on(chunkProvider).get("chunkLoader");
        ChunkRegionLoader regionLoader = ((ChunkRegionLoader) chunkLoader);

        return Reflect.on(regionLoader).get("d");
    }

    @Override
    public void setLight(World world, IntPosition position, int lightLevel) {
        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in World " + world.getName());
        }

        manager.setCustomLuminance(position, lightLevel);
    }

    @Override
    public void setLight(Location location, int lightLevel) {
        WorldLightSourceManager manager = plugin.getManager(location.getWorld());

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in World " + location.getWorld().getName());
        }

        manager.setCustomLuminance(location, lightLevel);
    }

    @Override
    public CompletableFuture<Void> setAndUpdateLight(World world, IntPosition position, int lightLevel) {
        setLight(world, position, lightLevel);

        return updateLight(world, position);
    }

    @Override
    public CompletableFuture<Void> setAndUpdateLight(Location location, int lightLevel) {
        return setAndUpdateLight(location.getWorld(), toIntPosition(location), lightLevel);
    }

    @Override
    public CompletableFuture<Void> updateLight(World world, IntPosition position) {

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in World " + world.getName());
        }

        final int lightLevel = manager.getCustomLuminance(position, 0);

        return CompletableFuture.runAsync(() -> {
            WorldServer nmsWorld = getNmsWorld(world);

            updateBlock0(position, lightLevel, nmsWorld);

            int sy = position.y / 16;
            int mask = 1 << sy;

            if (sy == 0) {
                mask |= 2;
            } else if (sy == 15) {
                mask |= 0x4000;
            } else {
                mask |= (1 << (sy - 1)) | (1 << (sy + 1));
            }

            PlayerChunkMap pcm = nmsWorld.getPlayerChunkMap();

            for (Chunk chunk : collectChunksToUpdate(position, world)) {
                PlayerChunk pc = pcm.getChunk(chunk.getX(), chunk.getZ());

                if (pc == null) {
                    continue;
                }

                for (int cy = 0; cy < 16; cy++) {
                    if ((mask & (1 << cy)) == 0) {
                        continue;
                    }

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                pc.a(x, cy * 16 + y, z);
                            }
                        }
                    }
                }

                pc.d();
            }

        }, plugin.getBukkitMainThreadExecutorService());
    }

    @Override
    public CompletableFuture<Void> updateLight(Location location) {
        return updateLight(location.getWorld(), toIntPosition(location));
    }

    @Override
    public CompletableFuture<Void> updateLight(World world, Collection<IntPosition> positions) {
        WorldServer nmsWorld = getNmsWorld(world);

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in world " + world.getName());
        }

        return CompletableFuture.runAsync(() -> {
            for (IntPosition position : positions) {
                updateBlock0(position, manager.getCustomLuminance(position, 0), nmsWorld);
            }
        }, plugin.getBukkitMainThreadExecutorService());
    }

    @Override
    public CompletableFuture<Void> recalculateChunk(Chunk chunk) {
        return recalculateChunk(chunk.getWorld(), new ChunkCoords(chunk.getX(), chunk.getZ()));
    }

    @Override
    public CompletableFuture<Void> recalculateChunk(World world, ChunkCoords chunkCoords) {
        WorldServer nmsWorld = getNmsWorld(world);

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in world " + world.getName());
        }

        return CompletableFuture.runAsync(() -> {
            StreamSupport.stream(BlockPosition.b(
                    chunkCoords.getCornerAX(),
                    chunkCoords.getCornerAY(),
                    chunkCoords.getCornerAZ(),
                    chunkCoords.getCornerBX(),
                    chunkCoords.getCornerBY(),
                    chunkCoords.getCornerBZ()
            ).spliterator(), false).forEach(mbp -> {
                IntPosition pos = new IntPosition(mbp.getX(), mbp.getY(), mbp.getZ());
                updateBlock0(pos, manager.getCustomLuminance(pos, 0), nmsWorld);
            });
        }, plugin.getBukkitMainThreadExecutorService());
    }

    @Override
    public void sendLightUpdates(World world, ChunkCoords center) {
        WorldServer nmsWorld = getNmsWorld(world);
        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in world " + world.getName());
        }

        PlayerChunkMap pcm = nmsWorld.getPlayerChunkMap();

        for (ChunkCoords toSendClientUpdate : collectChunkPositionsToUpdate(center)) {
            PlayerChunk pc = pcm.getChunk(toSendClientUpdate.x, toSendClientUpdate.z);

            if (pc == null) {
                continue;
            }

            pc.d();
        }
    }

    @Override
    public CompletableFuture<Void> updateChunk(World world, ChunkCoords chunkCoords) {
        return CompletableFuture.completedFuture(null); // TODO ?? What to do here in 1.12, we'll see during testing if this breaks stuff and it probably will
    }

    private void updateBlock0(IntPosition position, int lightLevel, WorldServer nmsWorld) {
        BlockPosition blockPosition = toBlockPosition(position);

        nmsWorld.c(EnumSkyBlock.BLOCK, blockPosition);

        if (lightLevel > 0) {
            nmsWorld.a(EnumSkyBlock.BLOCK, blockPosition, lightLevel);

            for (BlockFace blockFace : CHECK_FACES) {
                IntPosition relative = position.getRelative(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());

                if (relative.outOfBounds()) {
                    continue;
                }

                if (!nmsWorld.getType(blockPosition).getMaterial().blocksLight()) {
                    nmsWorld.c(EnumSkyBlock.BLOCK, blockPosition);
                }
            }
        }
    }

    // region Util

    private WorldServer getNmsWorld(World world) {
        return ((org.bukkit.craftbukkit.v1_12_R1.CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private BlockPosition toBlockPosition(IntPosition position) {
        return new BlockPosition(position.x, position.y, position.z);
    }

    //
}
