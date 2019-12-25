package me.shawlaf.varlight.nms;

import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.persistence.PersistentLightSource;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;
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

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

        if (!plugin.isLightApiInstalled()) {
            throw new VarLightInitializationException("LightAPI required!");
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean isInvalidLightUpdateItem(Material material) {
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
    public Collection<String> getBlockTypes() {
        Set<String> keys = new HashSet<>();

        for (MinecraftKey key : IRegistry.BLOCK.keySet()) {
            keys.add(key.toString());
            keys.add(key.getKey());
        }

        return keys;
    }

    @Override
    public Material blockTypeFromMinecraftKey(String key) {
        return CraftMagicNumbers.getMaterial(IRegistry.BLOCK.get(new MinecraftKey(key)));
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
//            WorldServer worldServer = getNmsWorld(theBlock.getWorld());

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

                if (customLuminance > 0 && theBlock.getLightFromBlocks() != customLuminance) {
                    updateBlockLight(relative, customLuminance);
                }
            }
        }, 1L);
    }

    // endregion
}
