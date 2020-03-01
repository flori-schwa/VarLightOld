package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.wrappers.WrappedILightAccess;
import me.shawlaf.varlight.spigot.persistence.PersistentLightSource;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.beykerykt.lightapi.chunks.ChunkInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

@SuppressWarnings("deprecation")
@ForMinecraft(version = "1.15.1")
public class NmsAdapter implements INmsAdapter, Listener {

    private static final BlockFace[] CHECK_FACES = new BlockFace[]{
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH
    };

    private final VarLightPlugin plugin;
    private final ItemStack varlightDebugStick;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

//        if (plugin.isLightApiMissing()) {
//            throw new VarLightInitializationException("LightAPI required!");
//        }

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
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onWorldEnable(@NotNull World world) {
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

    @Override
    public boolean isCorrectTool(Material block, Material tool) {
        net.minecraft.server.v1_15_R1.Block nmsBlock = CraftMagicNumbers.getBlock(block);
        Item item = CraftMagicNumbers.getItem(tool);

        net.minecraft.server.v1_15_R1.ItemStack stack = new net.minecraft.server.v1_15_R1.ItemStack(item);

        return item.getDestroySpeed(stack, nmsBlock.getBlockData()) > 1.0f;
    }

    @Override
    public String materialToKey(Material material) {
        return material.isBlock() ?
                IRegistry.BLOCK.getKey(CraftMagicNumbers.getBlock(material)).toString() :
                IRegistry.ITEM.getKey(CraftMagicNumbers.getItem(material)).toString();
    }

    @Override
    public String getLocalizedBlockName(Material material) {
        return LocaleLanguage.a().a(CraftMagicNumbers.getBlock(material).k());
    }

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
    public boolean isBlockTransparent(@NotNull Block block) {
        throw new RuntimeException("Not used in combination with LightAPI");
    }

    private void updateLight(WorldServer worldServer, ChunkCoords chunkCoords) {
        LightEngineThreaded let = worldServer.getChunkProvider().getLightEngine();

        let.a(worldServer.getChunkAt(chunkCoords.x, chunkCoords.z), false).thenRun(() -> {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
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
    public void updateBlockLight(@NotNull Location at, int lightLevel) {
        Objects.requireNonNull(at);
        Objects.requireNonNull(at.getWorld());

        WorldServer nmsWorld = getNmsWorld(at.getWorld());
        IntPosition pos = toIntPosition(at);
        BlockPosition blockPosition = new BlockPosition(pos.x, pos.y, pos.z);
        LightEngineThreaded let = nmsWorld.getChunkProvider().getLightEngine();
        LightEngineBlock leb = ((LightEngineBlock) let.a(EnumSkyBlock.BLOCK));

        leb.a(blockPosition);                       // Check Block
        updateLight(nmsWorld, pos.toChunkCoords()); // Update neighbouring Chunks and send updates to players

//        if (!LightAPI.deleteLight(at, false)) {
//            throw new LightUpdateFailedException("LightAPI not enabled or deleteLight Event cancelled!");
//        }
//
//        if (lightLevel > 0) {
//            if (!LightAPI.createLight(at, lightLevel, false)) {
//                throw new LightUpdateFailedException("LightAPI not enabled or createLight Event cancelled!");
//            }
//        }
//
//        List<Chunk> chunksToUpdate = collectChunksToUpdate(at);
//        List<ChunkInfo> chunkSectionsToUpdate = new ArrayList<>();
//
//        final int sectionY = at.getBlockY() >> 4;
//
//        for (Chunk chunk : chunksToUpdate) {
//            final int chunkX = chunk.getX(), chunkZ = chunk.getZ();
//
//            chunkSectionsToUpdate.add(toChunkInfo(at.getWorld(), chunkX, sectionY, chunkZ));
////            chunkSectionsToUpdate.add(new ChunkInfo(at.getWorld(), chunk.getX(), sectionY, chunk.getZ(), at.getWorld().getPlayers()));
//
//            if (sectionY < 16) {
//                chunkSectionsToUpdate.add(toChunkInfo(at.getWorld(), chunkX, sectionY + 1, chunkZ));
////                chunkSectionsToUpdate.add(new ChunkInfo(at.getWorld(), chunk.getX(), sectionY + 1, chunk.getZ(), at.getWorld().getPlayers()));
//            }
//
//            if (sectionY > 0) {
//                chunkSectionsToUpdate.add(toChunkInfo(at.getWorld(), chunkX, sectionY - 1, chunkZ));
////                chunkSectionsToUpdate.add(new ChunkInfo(at.getWorld(), chunk.getX(), sectionY - 1, chunk.getZ(), at.getWorld().getPlayers()));
//            }
//        }
//
//        for (ChunkInfo chunkInfo : chunkSectionsToUpdate) {
//            LightAPI.updateChunk(chunkInfo, LightType.BLOCK);
//        }
    }

    private ChunkInfo toChunkInfo(World world, int x, int sectionY, int z) {
        return new ChunkInfo(world, x, sectionY * 16, z, world.getPlayers());
    }

    @Override
    public int getVanillaLuminance(@NotNull Block block) {
        IBlockData blockData = ((CraftBlock) block).getNMS();

        return blockData.getBlock().a(blockData);
    }

    @Override
    public void sendChunkUpdates(@NotNull Chunk chunk, int mask) {
    }

    @Override
    public boolean isIllegalBlock(@NotNull Block block) {
        if (!block.getType().isBlock()) {
            return true;
        }

        if (this.getVanillaLuminance(block) > 0) {
            return true;
        }

        BlockData blockData = block.getType().createBlockData();

        if (blockData instanceof Powerable || blockData instanceof AnaloguePowerable || blockData instanceof Openable || blockData instanceof Piston) {
            return true;
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return true;
        }

        if (block.getType() == Material.BLUE_ICE) {
            return false; // Packed ice is solid and occluding but blue ice isn't?
        }

        return !block.getType().isSolid() || !block.getType().isOccluding();
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    @Override
    public Block getTargetBlockExact(Player player, int maxDistance) {
        return player.getTargetBlockExact(maxDistance);
    }

    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

    // region Util

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    // endregion

    // region Events

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        handleBlockUpdate(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        handleBlockUpdate(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent e) {
        handleBlockUpdate(e);
    }

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

    @Override
    public void handleBlockUpdate(BlockEvent e) {
//        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
//            Block theBlock = e.getBlock();
//
//            WorldLightSourceManager manager = plugin.getManager(theBlock.getWorld());
//
//            if (manager == null) {
//                return;
//            }
//
//            for (BlockFace blockFace : CHECK_FACES) {
//                Location relative = theBlock.getLocation().add(blockFace.getDirection());
//
//                PersistentLightSource pls = manager.getPersistentLightSource(relative);
//
//                if (pls == null) {
//                    continue;
//                }
//
//                int customLuminance = pls.getCustomLuminance();
//
//                if (customLuminance > 0) {
//                    updateLight(getNmsWorld(relative.getWorld()), toIntPosition(relative).toChunkCoords());
//                }
//            }
//        });
    }

    // endregion
}
