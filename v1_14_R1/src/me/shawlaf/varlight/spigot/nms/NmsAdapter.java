package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.wrappers.WrappedILightAccess;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.util.ReflectionHelper;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
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

@ForMinecraft(version = "1.14.x")
public class NmsAdapter implements INmsAdapter {

    private final VarLightPlugin plugin;
    private final ItemStack varlightDebugStick;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

        net.minecraft.server.v1_14_R1.ItemStack nmsStack = new net.minecraft.server.v1_14_R1.ItemStack(Items.STICK);

        nmsStack.addEnchantment(Enchantments.DURABILITY, 1);
        nmsStack.a("CustomType", new NBTTagString("varlight:debug_stick"));

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
    public String getLocalizedBlockName(Material material) {
        return LocaleLanguage.a().a(CraftMagicNumbers.getBlock(material).l());
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
    public void updateChunk(World world, ChunkCoords chunkCoords) {
        updateChunk(getNmsWorld(world), chunkCoords);
    }

    @Override
    public synchronized void updateBlocks(World world, ChunkCoords chunkCoords) {
        WorldServer nmsWorld = getNmsWorld(world);

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            return;
        }

        IBlockAccess blockAccess = nmsWorld.getChunkProvider().c(chunkCoords.x, chunkCoords.z);

        Function<BlockPosition, Integer> h = bPos -> manager.getCustomLuminance(new IntPosition(bPos.getX(), bPos.getY(), bPos.getZ()), () -> blockAccess.h(bPos));

        LightEngineBlock leb = ((LightEngineBlock) nmsWorld.getChunkProvider().getLightEngine().a(EnumSkyBlock.BLOCK));

        StreamSupport.stream(BlockPosition.b(
                chunkCoords.getCornerAX(),
                chunkCoords.getCornerAY(),
                chunkCoords.getCornerAZ(),
                chunkCoords.getCornerBX(),
                chunkCoords.getCornerBY(),
                chunkCoords.getCornerBZ()
        ).spliterator(), false).filter(bPos -> h.apply(bPos) > 0).forEach(leb::a);
    }

    private void updateChunk(WorldServer worldServer, ChunkCoords chunkCoords) {
        LightEngine let = worldServer.getChunkProvider().getLightEngine();

        WorldLightSourceManager manager = plugin.getManager(worldServer.getWorld());

        if (manager == null) {
            return;
        }

        IChunkAccess iChunkAccess = worldServer.getChunkProvider().a(chunkCoords.x, chunkCoords.z);

        ((LightEngineThreaded) let).a(iChunkAccess, false).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ChunkCoords toSendClientUpdate : collectChunkPositionsToUpdate(chunkCoords)) {
                    ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(toSendClientUpdate.x, toSendClientUpdate.z);
                    PacketPlayOutLightUpdate ppolu = new PacketPlayOutLightUpdate(chunkCoordIntPair, let);

                    worldServer.getChunkProvider().playerChunkMap.a(chunkCoordIntPair, false)
                            .forEach(e -> e.playerConnection.sendPacket(ppolu));
                }
            });
        });
    }

    @Override
    public void updateBlocksAndChunk(@NotNull Location at) {
        Objects.requireNonNull(at);
        Objects.requireNonNull(at.getWorld());

        WorldServer nmsWorld = getNmsWorld(at.getWorld());
        IntPosition pos = toIntPosition(at);
        BlockPosition blockPosition = new BlockPosition(pos.x, pos.y, pos.z);
        LightEngineThreaded let = nmsWorld.getChunkProvider().getLightEngine();
        LightEngineBlock leb = ((LightEngineBlock) let.a(EnumSkyBlock.BLOCK));

        leb.a(blockPosition);                       // Check Block
        updateChunk(nmsWorld, pos.toChunkCoords()); // Update neighbouring Chunks and send updates to players
    }

    @Override
    public boolean isIllegalBlock(@NotNull Material material) {
        return !(plugin.getAllowedBlocks().isTagged(material) || (plugin.getConfiguration().isAllowExperimentalBlocks() && plugin.getExperimentalBlocks().isTagged(material)));
    }

    @NotNull
    @Override
    public ItemStack getVarLightDebugStick() {
        net.minecraft.server.v1_14_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(varlightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.a("idLeast", new NBTTagLong(id.getLeastSignificantBits()));
        nmsStack.a("idMost", new NBTTagLong(id.getMostSignificantBits()));

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public ItemStack makeGlowingStack(ItemStack base, int lightLevel) {
        net.minecraft.server.v1_14_R1.ItemStack nmsStack = new net.minecraft.server.v1_14_R1.ItemStack(
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
        net.minecraft.server.v1_14_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(glowingStack);

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

        net.minecraft.server.v1_14_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

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

    @Override
    public String getDefaultLevelName() {
        return ((DedicatedServer) MinecraftServer.getServer()).propertyManager.getProperties().levelName;
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }
}
