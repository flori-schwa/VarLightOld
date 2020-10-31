package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.executor.AbstractBukkitExecutor;
import me.shawlaf.varlight.spigot.executor.BukkitAsyncExecutorService;
import me.shawlaf.varlight.spigot.executor.BukkitSyncExecutorService;
import me.shawlaf.varlight.spigot.executor.Ticks;
import me.shawlaf.varlight.spigot.nms.INmsAdapter;
import me.shawlaf.varlight.spigot.nms.VarLightInitializationException;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.persistence.migrate.LightDatabaseMigratorSpigot;
import me.shawlaf.varlight.spigot.persistence.migrate.data.JsonToNLSMigration;
import me.shawlaf.varlight.spigot.persistence.migrate.data.VLDBToNLSMigration;
import me.shawlaf.varlight.spigot.persistence.migrate.structure.MoveVarlightRootFolder;
import me.shawlaf.varlight.spigot.prompt.ChatPromptManager;
import me.shawlaf.varlight.spigot.util.IntPositionExtension;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.DoublePredicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;
import static me.shawlaf.varlight.spigot.util.LightSourceUtil.placeNewLightSource;

public class VarLightPlugin extends JavaPlugin implements Listener {

    private static final String SERVER_VERSION;

    public static final long TICK_RATE = 20L;
    private final Map<UUID, Integer> stepSizes = new HashMap<>();
    private final Map<UUID, WorldLightSourceManager> managers = new HashMap<>();
    private final INmsAdapter nmsAdapter;
    private final AbstractBukkitExecutor bukkitAsyncExecutorService, bukkitSyncExecutorService;
    private VarLightCommand command;
    private VarLightConfiguration configuration;
    private AutosaveManager autosaveManager;
    private DebugManager debugManager;
    private LightDatabaseMigratorSpigot databaseMigrator;
    private ChatPromptManager chatPromptManager;

    private Material lightUpdateItem;

    private boolean shouldDeflate;
    private boolean doLoad = true;

    static {
        String ver = Bukkit.getServer().getClass().getPackage().getName();
        SERVER_VERSION = ver.substring(ver.lastIndexOf('.') + 1);
    }

    {
        this.bukkitAsyncExecutorService = new BukkitAsyncExecutorService(this);
        this.bukkitSyncExecutorService = new BukkitSyncExecutorService(this);

        try {
            Class<?> nmsAdapterClass = Class.forName(String.format("me.shawlaf.varlight.spigot.nms.%s.NmsAdapter", SERVER_VERSION));

            this.nmsAdapter = (INmsAdapter) nmsAdapterClass.getConstructor(VarLightPlugin.class).newInstance(this);
        } catch (Throwable e) { // Catch anything that goes wrong while initializing, including reflection stuff
            String errMsg = String.format("Failed to initialize VarLight for Minecraft Version \"%s\": %s", Bukkit.getVersion(), e.getMessage());

            startupError(errMsg);
            throw new VarLightInitializationException(errMsg, e);
        }
    }

    @Override
    public void onLoad() {
        if (!doLoad) {
            return;
        }

        getLogger().info(String.format("Loading VarLight for Minecraft version \"%s\"", nmsAdapter.getForMinecraftVersion()));

        debugManager = new DebugManager(this);
        configuration = new VarLightConfiguration(this);
        databaseMigrator = new LightDatabaseMigratorSpigot(this);
        chatPromptManager = new ChatPromptManager(this);

//        nmsAdapter.addVarLightDatapackSource(Bukkit.getServer(), this);

        databaseMigrator.addDataMigrations(
                new JsonToNLSMigration(this),
                new VLDBToNLSMigration(this)
        );

        databaseMigrator.addStructureMigrations(
                new MoveVarlightRootFolder(this)
        );

        this.shouldDeflate = getConfig().getBoolean(VarLightConfiguration.CONFIG_KEY_NLS_DEFLATED, true);

        try {
            nmsAdapter.onLoad();
        } catch (VarLightInitializationException e) {
            doLoad = false;
            Bukkit.getPluginManager().disablePlugin(this);

            throw e;
        }
    }

    @Override
    public void onEnable() {
        if (!doLoad) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            nmsAdapter.onEnable();
        } catch (VarLightInitializationException e) {
            doLoad = false;
            Bukkit.getPluginManager().disablePlugin(this);

            throw e;
        }

//        nmsAdapter.enableDatapack(Bukkit.getServer(), INmsAdapter.DATAPACK_IDENT);

//        configuration.getVarLightEnabledWorlds().forEach(this::enableInWorld);

        loadLightUpdateItem();

        Bukkit.getPluginManager().registerEvents(this.autosaveManager = new AutosaveManager(this), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            Bukkit.getPluginManager().registerEvents(new WorldGuardExtension(), this);
        }

        command = new VarLightCommand(this);

        NumericMajorMinorVersion current = NumericMajorMinorVersion.tryParse(getDescription().getVersion());

        if (current != null && configuration.isCheckUpdateEnabled()) { // Development versions are not numeric
            bukkitAsyncExecutorService.submit(new UpdateCheck(getLogger(), current));
        }
    }

    @Override
    public void onDisable() {
        if (!doLoad) {
            return;
        }

        nmsAdapter.onDisable();

        for (WorldLightSourceManager l : getAllManagers()) {
            l.save(Bukkit.getConsoleSender(), configuration.isLogVerbose());
        }

        saveConfig();
    }

    public void enableInWorld(World world) {
        getLogger().info(String.format("Enabling in World [%s]", world.getName()));

        managers.put(
                world.getUID(),
                new WorldLightSourceManager(this, world)
        );

        nmsAdapter.enableVarLightInWorld(world);
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

    public INmsAdapter getNmsAdapter() {
        return nmsAdapter;
    }

    public AbstractBukkitExecutor getBukkitAsyncExecutorService() {
        return bukkitAsyncExecutorService;
    }

    public AbstractBukkitExecutor getBukkitMainThreadExecutorService() {
        return bukkitSyncExecutorService;
    }

    public VarLightCommand getCommand() {
        return command;
    }

    public VarLightConfiguration getConfiguration() {
        return configuration;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    public LightDatabaseMigratorSpigot getDatabaseMigrator() {
        return databaseMigrator;
    }

    public ChatPromptManager getChatPromptManager() {
        return chatPromptManager;
    }

    public void reload() {
        loadLightUpdateItem();
    }

    public AutosaveManager getAutosaveManager() {
        return autosaveManager;
    }

    public Material getLightUpdateItem() {
        return lightUpdateItem;
    }

    public void setUpdateItem(Material item) {
        getConfig().set(VarLightConfiguration.CONFIG_KEY_VARLIGHT_ITEM, nmsAdapter.getKey(item).toString());
        saveConfig();

        loadLightUpdateItem();
    }

    public boolean hasValidStepsizeGamemode(Player player) {
        return configuration.isAllowedStepsizeGamemode(player.getGameMode());
    }

    public void setStepSize(Player player, int stepSize) {
        if (stepSize < 1 || stepSize > 15) {
            throw new IllegalArgumentException("The Step size must be 1 <= n <= 15");
        }

        this.stepSizes.put(player.getUniqueId(), stepSize);
    }

    public boolean shouldDeflate() {
        return shouldDeflate;
    }

    // region Events

    @EventHandler
    public void worldLoad(WorldLoadEvent e) {
        if (configuration.getVarLightEnabledWorldNames().contains(e.getWorld().getName())) {
            enableInWorld(e.getWorld());
        }
    }

    @EventHandler
    public void playerModifyLightSource(PlayerInteractEvent e) {
        WorldLightSourceManager manager = getManager(e.getPlayer().getWorld());

        if (manager == null && configuration.getVarLightEnabledWorldNames().contains(e.getPlayer().getWorld().getName())) {
            enableInWorld(e.getPlayer().getWorld());

            manager = getManager(e.getPlayer().getWorld());
        }

        if (manager == null) {
            return;
        }

        if (e.getItem() != null && nmsAdapter.isVarLightDebugStick(e.getItem())) {
            e.setCancelled(true);

            if (!e.getPlayer().hasPermission("varlight.admin.debug")) {
                failure(command, e.getPlayer(), "You do not have permission to use the debug stick!");
                return;
            }

            if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            Block clickedBlock = e.getClickedBlock();

            int customLuminance = manager.getCustomLuminance(toIntPosition(clickedBlock), 0);

            if (customLuminance == 0) {
                info(command, e.getPlayer(),
                        String.format("No custom light source present at Position [%d, %d, %d]",
                                clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()),
                        ChatColor.RED
                );
            } else {
                info(command, e.getPlayer(), String.format("Custom Light Source with Light Level %d Present at Position %s", customLuminance, toIntPosition(clickedBlock).toShortString()), ChatColor.GREEN);
            }

            return;
        }

        if (e.useInteractedBlock() == Event.Result.DENY || (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK)) {
            return;
        }


        if (configuration.isCheckingPermission() && !e.getPlayer().hasPermission("varlight.use")) {
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

        if (hasValidStepsizeGamemode(player)) {
            mod *= stepSizes.getOrDefault(player.getUniqueId(), 1);
        }

        final boolean creative = player.getGameMode() == GameMode.CREATIVE;

        if (!creative) {
            if (heldItem.getAmount() < Math.abs(mod)) {
                return;
            }
        }

        LightUpdateResult result = placeNewLightSource(this, player, clickedBlock.getLocation(),
                manager.getCustomLuminance(toIntPosition(clickedBlock), 0) + mod);

        if (result.successful()) {
            e.setCancelled(creative && e.getAction() == Action.LEFT_CLICK_BLOCK);

            if (configuration.isConsumeLui() && !creative && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                heldItem.setAmount(heldItem.getAmount() - Math.abs(mod));
            }
        }

        int finalMod = mod;

        debugManager.logDebugAction(player,
                () -> "Edit Lightsource @ " + IntPositionExtension.toIntPosition(clickedBlock).toShortString() + " " +
                        (finalMod < 0 ? "LC" : "RC") + " " + result.getFromLight() + " -> " + result.getToLight() + " ==> " + result.getDebugMessage().toString()
        );

        result.displayMessage(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerBreakLightSource(BlockBreakEvent e) {
        Block theBlock = e.getBlock();
        World world = theBlock.getWorld();

        WorldLightSourceManager manager = getManager(world);

        if (manager == null) {
            return;
        }

        int emittingLight = manager.getCustomLuminance(toIntPosition(theBlock), -1);

        if (emittingLight <= 0) {
            return;
        }

        if (nmsAdapter.needsManualBreakCheck()) {
            // 1.12 Breaking and deleting of custom light sources handled here, 1.13+ handled in lightSourceReceiveUpdate
            nmsAdapter.setAndUpdateLight(e.getBlock().getLocation(), 0); // Delete the custom Light source from NLS
        }

        if (!configuration.isReclaimEnabled()) {
            return;
        }

        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand(); // You cannot break blocks with items in off hand

        int fortuneLvl = heldItem.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);

        Location dropLocation = theBlock.getLocation();

        if (heldItem.getEnchantmentLevel(Enchantment.SILK_TOUCH) != 0) {
            Collection<ItemStack> drops = theBlock.getDrops(heldItem);

            if (drops.size() != 1 || drops.stream().findFirst().get().getAmount() != 1) {
                return;
            }

            ItemStack dropStack = drops.stream().findFirst().get();
            dropStack = nmsAdapter.makeGlowingStack(dropStack, emittingLight);

            e.setDropItems(false);
            nmsAdapter.dropBlockItemNaturallyRespectGameruleAndEvents(world, dropLocation, e.getPlayer(), theBlock, dropStack);
        } else {
            if (!configuration.isConsumeLui()) {
                return; // Check for consume lui to prevent ez duping of lui
            }

            if (theBlock.getDrops(heldItem).size() == 0) {
                return;
            }

            ItemStack lui = new ItemStack(lightUpdateItem, 1);

            if (fortuneLvl == 0) {
                nmsAdapter.dropBlockItemNaturallyRespectGameruleAndEvents(world, dropLocation, e.getPlayer(), theBlock, lui);
            } else {
                // f(x) = 1 - (1 - -0.5) * e^(-0.6 * x) where x = fortuneLvl
                double chance = 1d - (1.5) * Math.exp(-0.6 * fortuneLvl);
                int dropCount = 1;

                for (int i = 1; i < emittingLight; i++) {
                    if (Math.random() <= chance) {
                        ++dropCount;
                    }
                }

                ItemStack[] drops = new ItemStack[dropCount];
                Arrays.fill(drops, lui);

                nmsAdapter.dropBlockItemNaturallyRespectGameruleAndEvents(world, dropLocation, e.getPlayer(), theBlock, drops);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerPlaceLightSource(BlockPlaceEvent e) {
        if (!configuration.isReclaimEnabled()) {
            return;
        }

        if (e.getItemInHand() == null) {
            return;
        }

        final ItemStack handCopy = e.getItemInHand().clone();
        final int emittingLight = nmsAdapter.getGlowingValue(handCopy);
        final Material before = e.getBlock().getType(); // We cannot always assume it's air, it could be water

        WorldLightSourceManager manager = getManager(e.getBlock().getWorld());

        if (emittingLight != -1) {
            if (manager == null && e.canBuild()) {
                info(command, e.getPlayer(), "VarLight is not active in your current world!");
                e.setCancelled(true);
                return;
            }

            if (!e.canBuild()) {
                return;
            }

            bukkitSyncExecutorService.submitDelayed(() -> {
                LightUpdateResult lightUpdateResult = placeNewLightSource(this, e.getPlayer(), e.getBlock().getLocation(), emittingLight);

                debugManager.logDebugAction(e.getPlayer(), () ->
                        "Place Lightsource (" + nmsAdapter.getKey(handCopy.getType()).toString() + ") @ " + IntPositionExtension.toIntPosition(e.getBlock()).toShortString() + " (" + emittingLight + "): " + lightUpdateResult.getDebugMessage().toString());

                if (!lightUpdateResult.successful()) {
                    e.getBlock().setType(before);

                    handCopy.setAmount(1);
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), handCopy);
                }
            }, Ticks.of(1));
        }
    }

    @EventHandler
    public void lightSourceReceiveUpdate(BlockPhysicsEvent e) {
        if (!nmsAdapter.hasBlockPhysicsSourceBlock()) {
            return;
        }

        WorldLightSourceManager manager = getManager(e.getBlock().getWorld());

        if (manager == null) {
            return;
        }

        IntPosition blockPos = toIntPosition(e.getBlock());
        int lum = manager.getCustomLuminance(blockPos, 0);

        if (lum > 0) {
            if (e.getBlock() == e.getSourceBlock()) { // WARN getSourceBlock() is a 1.13+ API Method!
                // The Light Source Block was changed

                // See World.notifyAndUpdatePhysics(BlockPosition, Chunk, IBlockData, IBlockData, IBlockData, int)
                if (nmsAdapter.isIllegalBlock(e.getChangedType())) {
                    manager.setCustomLuminance(blockPos, 0);
                } else {
                    // Probably not possible, but /shrug

                    bukkitSyncExecutorService.submit(() -> {
                        nmsAdapter.updateChunk(e.getBlock().getWorld(), blockPos.toChunkCoords());
                        nmsAdapter.sendLightUpdates(e.getBlock().getWorld(), blockPos.toChunkCoords());
                    });
                }
            } else {
                // The Light source Block received an update from another Block

                bukkitSyncExecutorService.submit(() -> {
                    nmsAdapter.updateChunk(e.getBlock().getWorld(), blockPos.toChunkCoords());
                    nmsAdapter.sendLightUpdates(e.getBlock().getWorld(), blockPos.toChunkCoords());
                });
            }
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e) {
        stepSizes.remove(e.getPlayer().getUniqueId());
    }

    // endregion

    private void startupError(String message) {
        getLogger().severe("------------------------------------------------------");
        getLogger().severe(message);
        getLogger().severe("------------------------------------------------------");

        doLoad = false;
    }

    private void loadLightUpdateItem() {
        this.lightUpdateItem = configuration.getLightUpdateItem();
        getLogger().info(String.format("Using \"%s\" as the Light update item.", nmsAdapter.getKey(lightUpdateItem).toString()));
    }

//    private void exportResource(String path, File toFile) {
//        byte[] buffer = new byte[8 * 1024];
//        int read;
//
//        try (InputStream in = getClass().getResourceAsStream(path)) {
//            try (FileOutputStream fos = new FileOutputStream(toFile)) {
//                while ((read = in.read(buffer, 0, buffer.length)) > 0) {
//                    fos.write(buffer, 0, read);
//                }
//            }
//        } catch (IOException e) {
//            throw new VarLightInitializationException(e.getMessage(), e);
//        }
//    }
}
