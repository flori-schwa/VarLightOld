package me.florian.varlight;

import me.florian.varlight.nms.NmsAdapter;
import me.florian.varlight.nms.NmsAdapter_1_12_R1;
import me.florian.varlight.nms.NmsAdapter_1_13_R2;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class VarLightPlugin extends JavaPlugin implements Listener {

    private static enum LightUpdateResult {
        INVALID_BLOCK,
        ZERO_REACHED,
        FIFTEEN_REACHED,
        UPDATED
    }

    private static Map<String, Class<? extends NmsAdapter>> ADAPTERS;

    private String serverVersion;
    private LightUpdater lightUpdater;
    private NmsAdapter nmsAdapter;
    private boolean doLoad = true;

    static {
        ADAPTERS = new HashMap<>();

        ADAPTERS.put("v1_13_R2", NmsAdapter_1_13_R2.class);
        ADAPTERS.put("v1_12_R1", NmsAdapter_1_12_R1.class);
    }

    @Override
    public void onLoad() {
        serverVersion = Bukkit.getServer().getClass().getPackage().getName();
        serverVersion = serverVersion.substring(serverVersion.lastIndexOf('.') + 1);

        if (! ADAPTERS.containsKey(serverVersion)) {
            getLogger().severe("------------------------------------------------------");
            getLogger().severe("Unspported Minecraft version: " + serverVersion);
            getLogger().severe("------------------------------------------------------");

            doLoad = false;
            return;
        }

        try {
            nmsAdapter = ADAPTERS.get(serverVersion).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            getLogger().throwing(getClass().getName(), "onLoad", e);
            doLoad = false;
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("LightAPI") != null) {
            getLogger().info("Using LightAPI");
            lightUpdater = new LightUpdaterLightAPI();
        } else {
            getLogger().info("Using Built-in Methods");
            lightUpdater = new LightUpdaterBuiltIn(this);
        }
    }

    @Override
    public void onEnable() {
        if (! doLoad) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public NmsAdapter getNmsAdapter() {
        return nmsAdapter;
    }

    private LightUpdateResult incrementLight(Block block) {
        if (! nmsAdapter.isValidBlock(block)) {
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
        if (! nmsAdapter.isValidBlock(block)) {
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
//                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Invalid Block."));
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
