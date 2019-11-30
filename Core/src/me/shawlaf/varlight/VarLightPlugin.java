package me.shawlaf.varlight;

import me.shawlaf.varlight.command.VarLightCommand;
import me.shawlaf.varlight.event.LightUpdateEvent;
import me.shawlaf.varlight.nms.*;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class VarLightPlugin extends JavaPlugin implements Listener {

    public static final NumericMajorMinorVersion
            MC1_14_2 = new NumericMajorMinorVersion("1.14.2");
    public static final long TICK_RATE = 20L;
    private static String PACKAGE_VERSION;

    private final Map<UUID, Integer> stepSizes = new HashMap<>();
    private final Map<UUID, WorldLightSourceManager> managers = new HashMap<>();

    private INmsAdapter nmsAdapter;
    private VarLightConfiguration configuration;
    private BukkitTask autosaveTask;
    private boolean doLoad = true;
    private PersistOnWorldSaveHandler persistOnWorldSaveHandler;
    private Material lightUpdateItem;

    public static String getPackageVersion() {
        return PACKAGE_VERSION;
    }

    private void unsupportedShutdown(String message) {
        getLogger().severe("------------------------------------------------------");
        getLogger().severe(message);
        getLogger().severe("");
        getLogger().severe("VarLight will shutdown the server!");
        getLogger().severe("Keeping the server running in this state");
        getLogger().severe("will result in countless bugs and errors in the console!");
        getLogger().severe("------------------------------------------------------");

        Bukkit.shutdown();

        doLoad = false;
    }

    @Override
    public void onLoad() {
        if ((int) ReflectionHelper.get(Bukkit.getServer(), "reloadCount") > 0) {
            unsupportedShutdown("VarLight does not support /reload!");
            return;
        }

        PACKAGE_VERSION = Bukkit.getServer().getClass().getPackage().getName();
        PACKAGE_VERSION = PACKAGE_VERSION.substring(PACKAGE_VERSION.lastIndexOf('.') + 1);

        final String version;

        try {
            version = NmsAdapter.class.getAnnotation(ForMinecraft.class).version();
            this.nmsAdapter = new NmsAdapter(this);
        } catch (Throwable e) { // Catch anything that goes wrong while initializing
            e.printStackTrace();
            unsupportedShutdown(String.format("Failed to initialize VarLight for Minecraft Version \"%s\": %s", Bukkit.getVersion(), e.getMessage()));
            return;
        }

        if (isLightApiInstalled() && !nmsAdapter.isLightApiAllowed()) {
            unsupportedShutdown(String.format("LightAPI is not allowed (%s)", version));
            return;
        }

        getLogger().info(String.format("Loading VarLight for Minecraft version \"%s\"", version));
        nmsAdapter.onLoad();
    }

    @Override
    public void onEnable() {
        if (!doLoad) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        configuration = new VarLightConfiguration(this);

        configuration.getVarLightEnabledWorlds().forEach(this::enableInWorld);

        try {
            nmsAdapter.onEnable();
        } catch (VarLightInitializationException e) {
            getLogger().throwing(getClass().getName(), "onEnable", e);
            Bukkit.getPluginManager().disablePlugin(this);
            doLoad = false;
            return;
        }

        loadLightUpdateItem();
        initAutosave();

        Bukkit.getPluginManager().registerEvents(this, this);

        final VarLightCommand handler = new VarLightCommand(this);
        final PluginCommand varlightPluginCommand = getCommand("varlight");

        varlightPluginCommand.setExecutor(handler);
        varlightPluginCommand.setTabCompleter(handler);
    }

    public boolean isLightApiInstalled() {
        return Bukkit.getPluginManager().getPlugin("LightAPI") != null;
    }

    public boolean isPaper() {
        return Package.getPackage("com.destroystokyo.paper") != null;
    }

    public void initAutosave() {
        if (autosaveTask != null && !autosaveTask.isCancelled()) {
            autosaveTask.cancel();
            autosaveTask = null;
        }

        if (persistOnWorldSaveHandler != null) {
            HandlerList.unregisterAll(persistOnWorldSaveHandler);
            persistOnWorldSaveHandler = null;
        }

        int saveInterval = configuration.getAutosaveInterval();

        if (saveInterval == 0) {
            getLogger().warning("Autosave is disabled! All Light sources will be lost if the server crashes and Light sources were not manually saved!");
            return;
        }

        if (saveInterval < 0) {
            persistOnWorldSaveHandler = new PersistOnWorldSaveHandler(this);

            Bukkit.getPluginManager().registerEvents(persistOnWorldSaveHandler, this);
            return;
        }

        long ticks = TimeUnit.MINUTES.toSeconds(saveInterval) * TICK_RATE;

        autosaveTask = Bukkit.getScheduler().runTaskTimer(this,
                () -> {
                    for (WorldLightSourceManager manager : getAllManagers()) {
                        manager.save(Bukkit.getConsoleSender());
                    }
                },
                ticks, ticks
        );
    }

    @Override
    public void onDisable() {
        if (!doLoad) {
            return;
        }

        nmsAdapter.onDisable();

        // If PersistOnSave is enabled, PersistOnWorldSaveHandler.onWorldSave will automatically save the Light Sources
        if (configuration.getAutosaveInterval() >= 0) {
            for (WorldLightSourceManager l : getAllManagers()) {
                l.save(Bukkit.getConsoleSender());
            }
        }
    }

    public void loadLightUpdateItem() {
        this.lightUpdateItem = configuration.getLightUpdateItem();
        getLogger().info(String.format("Using \"%s\" as the Light update item.", lightUpdateItem.name()));
    }

    public Material getLightUpdateItem() {
        return lightUpdateItem;
    }

    public void enableInWorld(World world) {
        managers.put(
                world.getUID(),
                new WorldLightSourceManager(this, world)
        );

        nmsAdapter.onWorldEnable(world);
    }

    public boolean hasManager(@NotNull World world) {
        return managers.containsKey(Objects.requireNonNull(world).getUID());
    }

    @NotNull
    public List<WorldLightSourceManager> getAllManagers() {
        return Collections.unmodifiableList(new ArrayList<>(managers.values()));
    }

    @Nullable
    public WorldLightSourceManager getManager(@NotNull World world) {
        return managers.get(Objects.requireNonNull(world).getUID());
    }

    public VarLightConfiguration getConfiguration() {
        return configuration;
    }

    public INmsAdapter getNmsAdapter() {
        return nmsAdapter;
    }

    private boolean isLightLevelInRange(int lightLevel) {
        return lightLevel >= 0 && lightLevel <= 15;
    }

    private boolean canModifyBlockLight(Block block, int mod) {
        return isLightLevelInRange(block.getLightFromBlocks() + mod);
    }

    private boolean isNullOrEmpty(String x) {
        return x == null || x.isEmpty();
    }

    public void setStepSize(Player player, int stepSize) {
        if (stepSize < 1 || stepSize > 15) {
            throw new IllegalArgumentException("The Step size must be 1 <= n <= 15");
        }

        this.stepSizes.put(player.getUniqueId(), stepSize);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        WorldLightSourceManager manager = getManager(e.getPlayer().getWorld());

        if (e.isCancelled() || e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (manager == null && configuration.getVarLightEnabledWorldNames().contains(e.getPlayer().getWorld().getName())) {
            enableInWorld(e.getPlayer().getWorld());

            manager = getManager(e.getPlayer().getWorld());
        }

        if (manager == null) {
            return;
        }

        String requiredPermission = configuration.getRequiredPermissionNode();

        if (!isNullOrEmpty(requiredPermission) && !e.getPlayer().hasPermission(requiredPermission)) {
            return;
        }

        Block clickedBlock = e.getClickedBlock();
        Player player = e.getPlayer();
        ItemStack heldItem = e.getItem();

        if (heldItem == null || heldItem.getType() != lightUpdateItem) {
            return;
        }

        int mod = 0;

        switch (e.getAction()) {
            case RIGHT_CLICK_BLOCK:
                mod = 1;
                break;
            case LEFT_CLICK_BLOCK:
                mod = -1;
                break;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            mod *= stepSizes.getOrDefault(player.getUniqueId(), 1);
        }

        final boolean creative = player.getGameMode() == GameMode.CREATIVE;

        if (nmsAdapter.isIllegalBlock(clickedBlock)) {
            displayMessage(player, LightUpdateResult.INVALID_BLOCK);
            return;
        }

        if (!canModifyBlockLight(clickedBlock, mod)) {
            displayMessage(player, mod < 0 ? LightUpdateResult.ZERO_REACHED : LightUpdateResult.FIFTEEN_REACHED);
            return;
        }

        LightUpdateEvent lightUpdateEvent = new LightUpdateEvent(this, clickedBlock, mod);
        Bukkit.getPluginManager().callEvent(lightUpdateEvent);

        if (lightUpdateEvent.isCancelled()) {
            displayMessage(player, LightUpdateResult.CANCELLED);
            return;
        }

        int lightTo = lightUpdateEvent.getToLight();

        nmsAdapter.updateBlockLight(clickedBlock.getLocation(), lightTo);
        manager.setCustomLuminance(clickedBlock.getLocation(), lightTo);

        e.setCancelled(creative && e.getAction() == Action.LEFT_CLICK_BLOCK);

        if (!creative && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        }

//        nmsAdapter.setCooldown(player, lightUpdateItem, 5);
        displayMessage(player, LightUpdateResult.UPDATED);
    }

    //    @EventHandler(priority = EventPriority.MONITOR)
//    public void onChunkLoad(ChunkLoadEvent e) {
//        WorldLightSourceManager manager = WorldLightSourceManager.getManager(this, e.getWorld());
//
//        if (manager != null) {
//            manager.loadChunk(e.getChunk());
//        }
//    }
//
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        WorldLightSourceManager manager = getManager(e.getWorld());

        if (manager != null) {
            manager.unloadChunk(e.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }

        WorldLightSourceManager manager = getManager(e.getBlock().getWorld());

        if (manager == null) {
            return;
        }

        IntPosition position = new IntPosition(e.getBlock().getLocation());

        if (manager.getCustomLuminance(position, -1) > 0) {
            manager.setCustomLuminance(position, 0); // Delete the light source
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        stepSizes.remove(e.getPlayer().getUniqueId());
    }

    private void displayMessage(Player player, LightUpdateResult lightUpdateResult) {
        switch (lightUpdateResult) {
            case CANCELLED:
            case INVALID_BLOCK: {
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
            }
        }
    }

    private enum LightUpdateResult {
        INVALID_BLOCK,
        ZERO_REACHED,
        FIFTEEN_REACHED,
        CANCELLED,
        UPDATED
    }
}
