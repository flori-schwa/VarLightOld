package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.wrappers.WrappedILightAccess;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

@SuppressWarnings("deprecation")
@ForMinecraft(version = "1.15.x")
public class NmsAdapter implements INmsAdapter {

    private final VarLightPlugin plugin;
    private final ItemStack varlightDebugStick;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

        net.minecraft.server.v1_15_R1.ItemStack nmsStack = new net.minecraft.server.v1_15_R1.ItemStack(Items.STICK);

        nmsStack.addEnchantment(Enchantments.DURABILITY, 1);
        nmsStack.a("CustomType", NBTTagString.a("varlight:debug_stick"));

        this.varlightDebugStick = CraftItemStack.asBukkitCopy(nmsStack);
        ItemMeta meta = varlightDebugStick.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "VarLight Debug Stick");
        varlightDebugStick.setItemMeta(meta);
    }

    @Override
    public void enableVarLightInWorld(@NotNull World world) {
        WorldServer nmsWorld = getNmsWorld(world);
        WrappedILightAccess wrappedILightAccess = new WrappedILightAccess(plugin, nmsWorld);

        LightEngineThreaded let = nmsWorld.getChunkProvider().getLightEngine();

        LightEngineBlock leb = ((LightEngineBlock) let.a(EnumSkyBlock.BLOCK));

        try {
            Field lightAccessField = LightEngineLayer.class.getDeclaredField("a");

            ReflectionHelper.Safe.set(leb, lightAccessField, wrappedILightAccess);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new VarLightInitializationException("Failed to inject custom ILightAccess into \"" + world.getName() + "\"'s Light Engine: " + e.getMessage(), e);
        }
    }

    @Override
    @Nullable
    public Material keyToType(String namespacedKey, MaterialType type) {
        MinecraftKey key = new MinecraftKey(namespacedKey);

        switch (type) {
            case ITEM: {
                return CraftMagicNumbers.getMaterial(IRegistry.ITEM.get(key));
            }

            case BLOCK: {
                return CraftMagicNumbers.getMaterial(IRegistry.BLOCK.get(key));
            }
        }

        return null;
    }

    @NotNull
    @Override
    public String materialToKey(Material material) {
        return material.isBlock() ?
                IRegistry.BLOCK.getKey(CraftMagicNumbers.getBlock(material)).toString() :
                IRegistry.ITEM.getKey(CraftMagicNumbers.getItem(material)).toString();
    }

    @NotNull
    @Override
    public String getLocalizedBlockName(Material material) {
        return LocaleLanguage.a().a(CraftMagicNumbers.getBlock(material).k());
    }

    @NotNull
    @Override
    public Collection<String> getTypes(MaterialType type) {
        List<String> types = new ArrayList<>();

        switch (type) {
            case ITEM: {
                for (MinecraftKey key : IRegistry.ITEM.keySet()) {
                    types.add(key.toString());
                    types.add(key.getKey());
                }

                return types;
            }

            case BLOCK: {
                for (MinecraftKey key : IRegistry.BLOCK.keySet()) {
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
    public void updateLight(World world, ChunkCoords chunkCoords) {
        updateLight(getNmsWorld(world), chunkCoords);
    }

    @Override
    public void updateBlocks(Chunk chunk) {
        WorldServer world = getNmsWorld(chunk.getWorld());
        ChunkCoords chunkCoords = new ChunkCoords(chunk.getX(), chunk.getZ());

        WorldLightSourceManager manager = plugin.getManager(chunk.getWorld());

        if (manager == null) {
            return;
        }

        IBlockAccess blockAccess = world.getChunkProvider().c(chunk.getX(), chunk.getZ());

        Function<BlockPosition, Integer> h = bPos -> manager.getCustomLuminance(new IntPosition(bPos.getX(), bPos.getY(), bPos.getZ()), () -> blockAccess.h(bPos));

        LightEngineBlock leb = ((LightEngineBlock) world.e().a(EnumSkyBlock.BLOCK));

        StreamSupport.stream(BlockPosition.b(
                chunkCoords.getCornerAX(),
                chunkCoords.getCornerAY(),
                chunkCoords.getCornerAZ(),
                chunkCoords.getCornerBX(),
                chunkCoords.getCornerBY(),
                chunkCoords.getCornerBZ()
        ).spliterator(), false).filter(bPos -> h.apply(bPos) > 0).forEach(leb::a);
    }

    private void updateLight(WorldServer worldServer, ChunkCoords chunkCoords) {
        LightEngine let = worldServer.e();

        WorldLightSourceManager manager = plugin.getManager(worldServer.getWorld());

        if (manager == null) {
            return;
        }

        IChunkAccess iChunkAccess = worldServer.getChunkProvider().a(chunkCoords.x, chunkCoords.z);

        ((LightEngineThreaded) let).a(iChunkAccess, false).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ChunkCoords toUpdate : collectChunkPositionsToUpdate(chunkCoords)) {
                    ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(toUpdate.x, toUpdate.z);
                    PacketPlayOutLightUpdate ppolu = new PacketPlayOutLightUpdate(chunkCoordIntPair, let);

                    worldServer.getChunkProvider().playerChunkMap.a(chunkCoordIntPair, false)
                            .forEach(e -> e.playerConnection.sendPacket(ppolu));
                }
            });
        });
    }

    @Override
    public void updateLight(@NotNull Location at, int lightLevel) { // TODO remove lightLevel parameter
        Objects.requireNonNull(at);
        Objects.requireNonNull(at.getWorld());

        WorldServer nmsWorld = getNmsWorld(at.getWorld());
        IntPosition pos = toIntPosition(at);
        BlockPosition blockPosition = new BlockPosition(pos.x, pos.y, pos.z);
        LightEngineThreaded let = nmsWorld.getChunkProvider().getLightEngine();
        LightEngineBlock leb = ((LightEngineBlock) let.a(EnumSkyBlock.BLOCK));

        leb.a(blockPosition);                       // Check Block
        updateLight(nmsWorld, pos.toChunkCoords()); // Update neighbouring Chunks and send updates to players
    }

    @Override
    public boolean isIllegalBlock(@NotNull Material material) {
        if (!material.isBlock()) {
            return true;
        }

        // If the Block is a vanilla Light source
        if (CraftMagicNumbers.getBlock(material).getBlockData().h() > 0) {
            return true;
        }

        BlockData blockData = material.createBlockData();

        if (blockData instanceof Powerable || blockData instanceof AnaloguePowerable || blockData instanceof Openable || blockData instanceof Piston) {
            return true;
        }

        if (material == Material.SLIME_BLOCK) {
            return true;
        }

        if (material == Material.BLUE_ICE) {
            return false;
        }

        return !material.isSolid() || !material.isOccluding();
    }

    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

    @NotNull
    @Override
    public ItemStack getVarLightDebugStick() {
        net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(varlightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.a("idLeast", NBTTagLong.a(id.getLeastSignificantBits()));
        nmsStack.a("idMost", NBTTagLong.a(id.getMostSignificantBits()));

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public ItemStack makeGlowingStack(ItemStack base, int lightLevel) {
        net.minecraft.server.v1_15_R1.ItemStack nmsStack = new net.minecraft.server.v1_15_R1.ItemStack(
                CraftMagicNumbers.getItem(base.getType()),
                base.getAmount()
        );

        lightLevel &= 0xF;

        nmsStack.a("varlight:glowing", NBTTagInt.a(lightLevel));

        ItemStack stack = CraftItemStack.asBukkitCopy(nmsStack);

        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Glowing " + getLocalizedBlockName(stack.getType()));
        meta.setLore(Collections.singletonList(ChatColor.RESET + "Emitting Light: " + lightLevel));

        stack.setItemMeta(meta);

        return stack;
    }

    @Override
    public int getGlowingValue(ItemStack glowingStack) {
        net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(glowingStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null || !tag.hasKey("varlight:glowing")) {
            return -1;
        }

        return tag.getInt("varlight:glowing");
    }

    @Override
    public boolean isVarLightDebugStick(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.STICK) {
            return false;
        }

        net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null) {
            return false;
        }

        return tag.getString("CustomType").equals("varlight:debug_stick");
    }

    @Override
    public @NotNull File getRegionRoot(World world) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();

        return nmsWorld.worldProvider.getDimensionManager().a(world.getWorldFolder());
    }

    // region Util

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    // endregion
}
