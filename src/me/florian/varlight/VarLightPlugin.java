package me.florian.varlight;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_13_R2.*;
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
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class VarLightPlugin extends JavaPlugin implements Listener {

    private static enum LightUpdateResult {
        INVALID_BLOCK,
        ZERO_REACHED,
        FIFTEEN_REACHED,
        UPDATED
    }

    private static final BlockFace[] CHECK_FACES = new BlockFace[] {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private boolean dependencyFound = true;
    private Field nField;

    @Override
    public void onLoad() {
        try {
            nField = net.minecraft.server.v1_13_R2.Block.class.getDeclaredField("n");
            nField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (dependencyFound) {
            Bukkit.getPluginManager().registerEvents(this, this);
        } else {
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private boolean isBlockTransparent(Block block) {
        try {
            return !nField.getBoolean(getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean emitsLight(Block block) {
        return ((CraftWorld) block.getWorld()).getHandle().getChunkAt(block.getChunk().getX(), block.getChunk().getZ()).getBlockData(block.getX(), block.getY(), block.getZ()).e() > 0;
    }

    private boolean isValidBlock(Block block) {
        if (! block.getType().isBlock()) {
            return false;
        }

        if (emitsLight(block)) {
            return false;
        }

        BlockData blockData = block.getType().createBlockData();

        if (blockData instanceof Powerable || blockData instanceof AnaloguePowerable || blockData instanceof Openable || blockData instanceof Piston) {
            return false;
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return false;
        }

        if (block.getType() == Material.BLUE_ICE) {
            return true; // Packed ice is solid and occluding but blue ice isn't?
        }

        return block.getType().isSolid() && block.getType().isOccluding();
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private void setLight(Location location, int lightLevel) {
        WorldServer world = getNmsWorld(location.getWorld());
        BlockPosition updateAt = toBlockPosition(location);
        Block block = location.getBlock();


        world.c(EnumSkyBlock.BLOCK, updateAt);

        if (lightLevel > 0) {
            world.a(EnumSkyBlock.BLOCK, updateAt, lightLevel);

            IntPosition intPosition = new IntPosition(block.getLocation());

            for (BlockFace blockFace : CHECK_FACES) {
                IntPosition relative = intPosition.getRelative(blockFace);

                if (relative.outOfBounds()) {
                    continue;
                }



                if (isBlockTransparent(relative.toBlock(location.getWorld()))) {
                    world.c(EnumSkyBlock.BLOCK, relative.toBlockPosition());
                    break;
                }
            }
        }


        int chunkX = location.getBlockX() / 16;
        int chunkZ = location.getBlockZ() / 16;

        List<ChunkCoordIntPair> chunksToUpdate = new ArrayList<>();

        for (int dx = - 1; dx <= 1; dx++) {
            for (int dz = - 1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;

                if (! world.getChunkProvider().isLoaded(x, z)) {
                    continue;
                }

                chunksToUpdate.add(new ChunkCoordIntPair(x, z));
            }
        }

        PlayerChunkMap playerChunkMap = world.getPlayerChunkMap();

        for (ChunkCoordIntPair chunkCoordIntPair : chunksToUpdate) {
            PlayerChunk playerChunk = playerChunkMap.getChunk(chunkCoordIntPair.x, chunkCoordIntPair.z);

            for (EntityPlayer entityPlayer : playerChunk.players) {
                entityPlayer.playerConnection.sendPacket(new PacketPlayOutMapChunk(playerChunk.chunk, (1 << 17) - 1));
            }
        }
    }


    private LightUpdateResult incrementLight(Block block) {
        if (! isValidBlock(block)) {
            return LightUpdateResult.INVALID_BLOCK;
        }

        int currentLight = block.getLightFromBlocks();

        if (currentLight == 15) {
            return LightUpdateResult.FIFTEEN_REACHED;
        }

        setLight(block.getLocation(), ++ currentLight);
        return LightUpdateResult.UPDATED;
    }

    private LightUpdateResult decrementLight(Block block) {
        if (! isValidBlock(block)) {
            return LightUpdateResult.INVALID_BLOCK;
        }

        int currentLight = block.getLightFromBlocks();

        if (currentLight == 0) {
            return LightUpdateResult.ZERO_REACHED;
        }

        setLight(block.getLocation(), -- currentLight);
        return LightUpdateResult.UPDATED;
    }


    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.isCancelled() || (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        Block clickedBlock = e.getClickedBlock();
        Player player = e.getPlayer();
        ItemStack heldItem = e.getItem();

        if (heldItem == null) {
            return;
        }

        if (heldItem.getType() != Material.GLOWSTONE_DUST) {
            return;
        }

        LightUpdateResult lightUpdateResult = null;

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            lightUpdateResult = incrementLight(clickedBlock);

            if (player.getGameMode() != GameMode.CREATIVE && lightUpdateResult == LightUpdateResult.UPDATED) {
                heldItem.setAmount(heldItem.getAmount() - 1);
            }
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            lightUpdateResult = decrementLight(clickedBlock);

            if (player.getGameMode() == GameMode.CREATIVE) {
                e.setCancelled(true);
            }
        }

        if (lightUpdateResult == null) {
            return;
        }

        switch (lightUpdateResult) {
            case INVALID_BLOCK: {
                return;
            }

            case FIFTEEN_REACHED: {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Cannot increase light level beyond 15."));
                return;
            }
            case ZERO_REACHED: {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Cannot decrease light level below 0."));
                return;
            }

            case UPDATED: {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Updated Light level of block to " + clickedBlock.getLightFromBlocks()));
                return;
            }
        }
    }
}
