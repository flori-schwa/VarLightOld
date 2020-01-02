package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.persistence.PersistentLightSource;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_14_R1.*;
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
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
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
import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.LightType;
import ru.beykerykt.lightapi.chunks.ChunkInfo;

import java.util.*;

@SuppressWarnings("deprecation")
@ForMinecraft(version = "1.14+")
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

        if (!plugin.isLightApiInstalled()) {
            throw new VarLightInitializationException("LightAPI required!");
        }

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
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
    public String materialToKey(Material material) {
        return material.isBlock() ?
                IRegistry.BLOCK.getKey(CraftMagicNumbers.getBlock(material)).toString() :
                IRegistry.ITEM.getKey(CraftMagicNumbers.getItem(material)).toString();
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

    @Override
    public void updateBlockLight(@NotNull Location at, int lightLevel) {
        Objects.requireNonNull(at);
        Objects.requireNonNull(at.getWorld());

        if (!LightAPI.deleteLight(at, false)) {
            throw new LightUpdateFailedException("LightAPI not enabled or deleteLight Event cancelled!");
        }

        if (lightLevel > 0) {
            if (!LightAPI.createLight(at, lightLevel, false)) {
                throw new LightUpdateFailedException("LightAPI not enabled or createLight Event cancelled!");
            }
        }

        List<Chunk> chunksToUpdate = collectChunksToUpdate(at);
        List<ChunkInfo> chunkSectionsToUpdate = new ArrayList<>();

        final int sectionY = at.getBlockY() >> 4;

        for (Chunk chunk : chunksToUpdate) {
            final int chunkX = chunk.getX(), chunkZ = chunk.getZ();

            chunkSectionsToUpdate.add(toChunkInfo(at.getWorld(), chunkX, sectionY, chunkZ));
//            chunkSectionsToUpdate.add(new ChunkInfo(at.getWorld(), chunk.getX(), sectionY, chunk.getZ(), at.getWorld().getPlayers()));

            if (sectionY < 16) {
                chunkSectionsToUpdate.add(toChunkInfo(at.getWorld(), chunkX, sectionY + 1, chunkZ));
//                chunkSectionsToUpdate.add(new ChunkInfo(at.getWorld(), chunk.getX(), sectionY + 1, chunk.getZ(), at.getWorld().getPlayers()));
            }

            if (sectionY > 0) {
                chunkSectionsToUpdate.add(toChunkInfo(at.getWorld(), chunkX, sectionY - 1, chunkZ));
//                chunkSectionsToUpdate.add(new ChunkInfo(at.getWorld(), chunk.getX(), sectionY - 1, chunk.getZ(), at.getWorld().getPlayers()));
            }
        }

        for (ChunkInfo chunkInfo : chunkSectionsToUpdate) {
            LightAPI.updateChunk(chunkInfo, LightType.BLOCK);
        }
    }

    private ChunkInfo toChunkInfo(World world, int x, int sectionY, int z) {
        return new ChunkInfo(world, x, sectionY * 16, z, world.getPlayers());
    }

    @Override
    public int getEmittingLightLevel(@NotNull Block block) {
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

        if (getEmittingLightLevel(block) > 0) {
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
    public ItemStack getVarLightDebugStick() {
        net.minecraft.server.v1_14_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(varlightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.a("idLeast", new NBTTagLong(id.getLeastSignificantBits()));
        nmsStack.a("idMost", new NBTTagLong(id.getMostSignificantBits()));

        return CraftItemStack.asBukkitCopy(nmsStack);
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
    public Block getTargetBlockExact(Player player, int maxDistance) {
        return player.getTargetBlockExact(maxDistance);
    }

    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

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

    private void handleBlockUpdate(BlockEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block theBlock = e.getBlock();

            WorldLightSourceManager manager = plugin.getManager(theBlock.getWorld());

            if (manager == null) {
                return;
            }


            for (BlockFace blockFace : CHECK_FACES) {
                Location relative = theBlock.getLocation().add(blockFace.getDirection());

                PersistentLightSource pls = manager.getPersistentLightSource(relative);

                if (pls == null) {
                    continue;
                }

                int customLuminance = pls.getCustomLuminance();

                if (pls.isInvalid() && areKeysEqual(materialToKey(relative.getBlock().getType()), pls.getType())) {
                    updateBlockLight(relative, customLuminance);
                }
            }
        }, 1L);
    }

    // endregion
}
