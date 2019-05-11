package me.florian.varlight;

import me.florian.varlight.nms.*;
import me.florian.varlight.nms.persistence.LightSourcePersistor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VarLightPlugin extends JavaPlugin implements Listener {

    public static final int DEFAULT_AUTOSAVE_INTERVALL = 5;
    public static final long TICK_RATE = 20L;

    private enum LightUpdateResult {
        INVALID_BLOCK,
        ZERO_REACHED,
        FIFTEEN_REACHED,
        CANCELLED,
        UPDATED;
    }

    private static String SERVER_VERSION;

    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    private static Map<String, Class<? extends NmsAdapter>> ADAPTERS;

    private LightUpdater lightUpdater;

    private NmsAdapter nmsAdapter;
    private boolean doLoad = true;

    static {
        ADAPTERS = new HashMap<>();

        ADAPTERS.put("v1_14_R1", NmsAdapter_1_14_R1.class);
        ADAPTERS.put("v1_13_R2", NmsAdapter_1_13_R2.class);
        ADAPTERS.put("v1_12_R1", NmsAdapter_1_12_R1.class);
        ADAPTERS.put("v1_11_R1", NmsAdapter_1_11_R1.class);
        ADAPTERS.put("v1_10_R1", NmsAdapter_1_10_R1.class);
        ADAPTERS.put("v1_9_R2", NmsAdapter_1_9_R2.class);
        ADAPTERS.put("v1_8_R3", NmsAdapter_1_8_R3.class);
    }

    public boolean isDebug() {
        return getDescription().getVersion().endsWith("-INDEV");
    }

    @Override
    public void onLoad() {
        SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName();
        SERVER_VERSION = SERVER_VERSION.substring(SERVER_VERSION.lastIndexOf('.') + 1);

        if (! ADAPTERS.containsKey(SERVER_VERSION)) {
            getLogger().severe("------------------------------------------------------");
            getLogger().severe("Unsupported Minecraft version: " + SERVER_VERSION);
            getLogger().severe("------------------------------------------------------");

            doLoad = false;
            return;
        }

        try {
            nmsAdapter = ADAPTERS.get(SERVER_VERSION).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NmsInitializationException e) {
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

        nmsAdapter.onLoad(this, lightUpdater instanceof LightUpdaterBuiltIn);
    }

    @Override
    public void onEnable() {
        if (! doLoad) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getWorlds().forEach(w -> LightSourcePersistor.getPersistor(this, w));
        nmsAdapter.onEnable(this, lightUpdater instanceof LightUpdaterBuiltIn);

        int saveInterval = getConfig().getInt("autosave", DEFAULT_AUTOSAVE_INTERVALL);

        if (saveInterval <= 0) {
            saveInterval = DEFAULT_AUTOSAVE_INTERVALL;
        }

        long ticks = TimeUnit.MINUTES.toSeconds(saveInterval) * TICK_RATE;

        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> LightSourcePersistor.getAllPersistors(this).forEach(p -> p.save(Bukkit.getConsoleSender())),
                ticks, ticks
        );
    }

    @Override
    public void onDisable() {
        if (! doLoad) {
            return;
        }

        nmsAdapter.onDisable(lightUpdater instanceof LightUpdaterBuiltIn);
        LightSourcePersistor.getAllPersistors(this).forEach(l -> l.save(Bukkit.getConsoleSender()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("save-lights".equals(command.getName()) && sender.hasPermission(command.getPermission())) {
            if (args.length == 1 && "all".equalsIgnoreCase(args[0])) {
                LightSourcePersistor.getAllPersistors(this).forEach(l -> l.save(sender));
            } else if (sender instanceof Player) {
                Player player = (Player) sender;
                LightSourcePersistor.getPersistor(this, player.getWorld()).save(sender);
            }

            return true;
        }

        if ("set-permission".equals(command.getName()) && sender.hasPermission(command.getPermission())) {
            if (args.length != 1) {
                return false;
            }

            getConfig().set("requiredPermission", args[0]);
            saveConfig();
            sender.sendMessage("Permission required updated!");
            return true;
        }

        return true;
    }

    public NmsAdapter getNmsAdapter() {
        return nmsAdapter;
    }

    public LightUpdater getLightUpdater() {
        return lightUpdater;
    }

    public void setLightUpdater(LightUpdater lightUpdater) {
        this.lightUpdater = lightUpdater;
    }

    private boolean isLightLevelInRange(int lightLevel) {
        return lightLevel >= 0 && lightLevel <= 15;
    }

    private boolean canModifyBlockLight(Block block, int mod) {
        return isLightLevelInRange(block.getLightFromBlocks() + mod);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.isCancelled() || (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) || nmsAdapter.hasCooldown(e.getPlayer(), Material.GLOWSTONE_DUST)) {
            return;
        }

        String requiredPermission = getConfig().getString("requiredPermission", null);

        if (requiredPermission != null && ! e.getPlayer().hasPermission(requiredPermission)) {
            return;
        }

        Block clickedBlock = e.getClickedBlock();
        Player player = e.getPlayer();
        ItemStack heldItem = e.getItem();

        if (heldItem == null || heldItem.getType() != Material.GLOWSTONE_DUST) {
            return;
        }

        int mod = 0;

        switch (e.getAction()) {
            case RIGHT_CLICK_BLOCK:
                mod = 1;
                break;
            case LEFT_CLICK_BLOCK:
                mod = - 1;
                break;
        }

        boolean creative = player.getGameMode() == GameMode.CREATIVE;

        if (! nmsAdapter.isValidBlock(clickedBlock)) {
            displayMessage(player, LightUpdateResult.INVALID_BLOCK);
            return;
        }

        if (! canModifyBlockLight(clickedBlock, mod)) {
            displayMessage(player, mod < 0 ? LightUpdateResult.ZERO_REACHED : LightUpdateResult.FIFTEEN_REACHED);
            return;
        }

        LightUpdateEvent lightUpdateEvent = new LightUpdateEvent(this, clickedBlock, mod);
        Bukkit.getPluginManager().callEvent(lightUpdateEvent);

        if (lightUpdateEvent.isCancelled()) {
            displayMessage(player, LightUpdateResult.CANCELLED);
            return;
        }

        int lightTo = lightUpdateEvent.getToLight() & 0xF;

        LightSourcePersistor.getPersistor(this, clickedBlock.getWorld())
                .getOrCreatePersistentLightSource(new IntPosition(clickedBlock.getLocation()))
                .setEmittingLight(lightTo);

        lightUpdater.setLight(clickedBlock.getLocation(), lightTo);

        e.setCancelled(creative && e.getAction() == Action.LEFT_CLICK_BLOCK);

        if (! creative && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        }

        nmsAdapter.setCooldown(player, Material.GLOWSTONE_DUST, DEFAULT_AUTOSAVE_INTERVALL);
        displayMessage(player, LightUpdateResult.UPDATED);
    }

    private void displayMessage(Player player, LightUpdateResult lightUpdateResult) {
        switch (lightUpdateResult) {

            case CANCELLED: {
                if (isDebug()) {
                    nmsAdapter.sendActionBarMessage(player, "Cancelled");
                }
            }

            case INVALID_BLOCK: {
                if (isDebug()) {
                    nmsAdapter.sendActionBarMessage(player, "Invalid Block.");
                }

                return;
            }

            case FIFTEEN_REACHED: {
                nmsAdapter.sendActionBarMessage(player, "Cannot increase light level beyond 15.");
                return;
            }
            case ZERO_REACHED: {
                nmsAdapter.sendActionBarMessage(player, "Cannot decrease light level below 0.");
                return;
            }

            case UPDATED: {
                nmsAdapter.sendActionBarMessage(player, "Updated Light level");
                return;
            }
        }
    }
}
