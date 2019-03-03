package me.florian.varlight;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class VarLightPlugin extends JavaPlugin implements Listener {

    private static enum LightUpdateResult {
        INVALID_BLOCK,
        ZERO_REACHED,
        FIFTEEN_REACHED,
        UPDATED
    }

    private LightUpdater lightUpdater;

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("LightAPI") != null) {
            getLogger().info("Using LightAPI");
            lightUpdater = new LightUpdaterLightAPI();
        } else {
            getLogger().info("Using Built-in Methods");
            lightUpdater = new LightUpdaterBuiltIn();
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
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


    private LightUpdateResult incrementLight(Block block) {
        if (! isValidBlock(block)) {
            return LightUpdateResult.INVALID_BLOCK;
        }

        int currentLight = block.getLightFromBlocks();

        if (currentLight == 15) {
            return LightUpdateResult.FIFTEEN_REACHED;
        }

        lightUpdater.setLight(block.getLocation(), ++ currentLight);
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

        lightUpdater.setLight(block.getLocation(), -- currentLight);
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
