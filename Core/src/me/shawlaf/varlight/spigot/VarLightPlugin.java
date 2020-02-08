package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.spigot.command.VarLightCommand;
import me.shawlaf.varlight.spigot.nms.INmsAdapter;
import me.shawlaf.varlight.spigot.nms.NmsAdapter;
import me.shawlaf.varlight.spigot.nms.VarLightInitializationException;
import me.shawlaf.varlight.spigot.persistence.PersistentLightSource;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.persistence.migrate.LightDatabaseMigrator;
import me.shawlaf.varlight.spigot.persistence.migrate.data.JsonToVLDBMigration;
import me.shawlaf.varlight.spigot.persistence.migrate.data.VLDBMigration;
import me.shawlaf.varlight.spigot.persistence.migrate.structure.MoveVarlightRootFolder;
import me.shawlaf.varlight.util.IntPosition;
import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;
import static me.shawlaf.varlight.spigot.util.LightSourceUtil.canNewLightSourceBePlaced;
import static me.shawlaf.varlight.spigot.util.LightSourceUtil.placeNewLightSource;

public class VarLightPlugin extends JavaPlugin implements Listener {

    public static final NumericMajorMinorVersion
            MC1_14_2 = new NumericMajorMinorVersion("1.14.2");
    public static final NumericMajorMinorVersion MC1_9 = new NumericMajorMinorVersion("1.9");
    public static final NumericMajorMinorVersion MC1_12 = new NumericMajorMinorVersion("1.12");
    public static final long TICK_RATE = 20L;

    private final Map<UUID, Integer> stepSizes = new HashMap<>();
    private final Map<UUID, WorldLightSourceManager> managers = new HashMap<>();
    private final INmsAdapter nmsAdapter;

    private VarLightCommand command;
    private VarLightConfiguration configuration;
    private AutosaveManager autosaveManager;

    private Material lightUpdateItem;
    private GameMode stepsizeGamemode;

    private boolean shouldVLDBDeflate;
    private boolean doLoad = true;

    {
        try {
            this.nmsAdapter = new NmsAdapter(this);
        } catch (Throwable e) { // Catch anything that goes wrong while initializing
            unsupportedOperation(String.format("Failed to initialize VarLight for Minecraft Version \"%s\": %s", Bukkit.getVersion(), e.getMessage()));
            throw e;
        }
    }

    @Override
    public void onLoad() {
        getLogger().info(String.format("Loading VarLight for Minecraft version \"%s\"", nmsAdapter.getForMinecraftVersion()));
        nmsAdapter.onLoad();
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

        LightDatabaseMigrator.addDataMigration(new JsonToVLDBMigration(this));
        LightDatabaseMigrator.addDataMigration(new VLDBMigration(this));

        LightDatabaseMigrator.addStructureMigration(new MoveVarlightRootFolder(this));

        this.shouldVLDBDeflate = getConfig().getBoolean(VarLightConfiguration.CONFIG_KEY_VLDB_DEFLATED, true);

        configuration = new VarLightConfiguration(this);
        configuration.getVarLightEnabledWorlds().forEach(this::enableInWorld);

        loadLightUpdateItem();
        loadStepsizeGamemode();

        Bukkit.getPluginManager().registerEvents(this.autosaveManager = new AutosaveManager(this), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        command = new VarLightCommand(this);

        NumericMajorMinorVersion current = NumericMajorMinorVersion.tryParse(getDescription().getVersion());

        if (current != null && configuration.isCheckUpdateEnabled()) { // Development versions are not numeric
            Bukkit.getScheduler().runTaskAsynchronously(this, new UpdateCheck(getLogger(), current));
        }
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
                l.save(Bukkit.getConsoleSender(), configuration.isLogDebug());
            }
        }

        saveConfig();
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

    public INmsAdapter getNmsAdapter() {
        return nmsAdapter;
    }

    public VarLightCommand getCommand() {
        return command;
    }

    public VarLightConfiguration getConfiguration() {
        return configuration;
    }

    public void reload() {
        loadLightUpdateItem();
        loadStepsizeGamemode();
    }

    public AutosaveManager getAutosaveManager() {
        return autosaveManager;
    }

    public Material getLightUpdateItem() {
        return lightUpdateItem;
    }

    public void setUpdateItem(Material item) {
        getConfig().set(VarLightConfiguration.CONFIG_KEY_VARLIGHT_ITEM, nmsAdapter.materialToKey(item));
        saveConfig();

        loadLightUpdateItem();
    }

    public boolean hasValidStepsizeGamemode(Player player) {
        switch (stepsizeGamemode) {
            case CREATIVE: {
                return player.getGameMode() == GameMode.CREATIVE;
            }

            case SURVIVAL: {
                return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SURVIVAL;
            }

            case ADVENTURE: {
                return player.getGameMode() != GameMode.SPECTATOR;
            }

            default:
            case SPECTATOR: {
                throw new IllegalStateException();
            }
        }
    }

    public void setStepSize(Player player, int stepSize) {
        if (stepSize < 1 || stepSize > 15) {
            throw new IllegalArgumentException("The Step size must be 1 <= n <= 15");
        }

        this.stepSizes.put(player.getUniqueId(), stepSize);
    }

    public boolean shouldVLDBDeflate() {
        return shouldVLDBDeflate;
    }

    public boolean isLightApiMissing() {
        return Bukkit.getPluginManager().getPlugin("LightAPI") == null;
    }

    // region Events

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        WorldLightSourceManager manager = getManager(e.getPlayer().getWorld());

        if (manager == null && configuration.getVarLightEnabledWorldNames().contains(e.getPlayer().getWorld().getName())) {
            enableInWorld(e.getPlayer().getWorld());

            manager = getManager(e.getPlayer().getWorld());
        }

        if (manager == null) {
            return;
        }

        if (nmsAdapter.isVarLightDebugStick(e.getItem())) {
            e.setCancelled(true);

            if (!e.getPlayer().hasPermission("varlight.admin.debug")) {
                failure(command, e.getPlayer(), "You do not have permission to use the debug stick!");
                return;
            }

            if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
                if (e.getAction() == Action.RIGHT_CLICK_AIR) {
                    if (e.getPlayer().isSneaking()) {
                        Bukkit.dispatchCommand(e.getPlayer(), "varlight debug list -r");
                    } else {
                        Bukkit.dispatchCommand(e.getPlayer(), "varlight debug list -c");
                    }

                    return;
                }

                return;
            }

            Block clickedBlock = e.getClickedBlock();

            PersistentLightSource pls = manager.getPersistentLightSource(clickedBlock.getLocation());

            if (pls == null) {
                info(command, e.getPlayer(),
                        String.format("No custom light source present at Position [%d, %d, %d]",
                                clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()),
                        ChatColor.RED
                );
            } else {
                info(command, e.getPlayer(), pls.toCompactString(true), ChatColor.GREEN);
            }

            return;
        }

        if (e.isCancelled() || (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK)) {
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

        if (hasValidStepsizeGamemode(player)) {
            mod *= stepSizes.getOrDefault(player.getUniqueId(), 1);
        }

        final boolean creative = player.getGameMode() == GameMode.CREATIVE;

        if (!creative) {
            if (heldItem.getAmount() < Math.abs(mod)) {
                return;
            }
        }

        LightUpdateResult result = placeNewLightSource(this, clickedBlock.getLocation(),
                manager.getCustomLuminance(toIntPosition(clickedBlock), 0) + mod);

        if (result.successful()) {
            e.setCancelled(creative && e.getAction() == Action.LEFT_CLICK_BLOCK);

            if (!creative && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                heldItem.setAmount(heldItem.getAmount() - Math.abs(mod));
            }
        }

        result.displayMessage(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        WorldLightSourceManager manager = getManager(e.getWorld());

        if (manager != null) {
            manager.unloadChunk(e.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!configuration.hasReclaim()) {
            handleBlockAffected(e.getBlock());
            return;
        }

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

        handleBlockAffected(theBlock);

        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack heldItem;

        if (nmsAdapter.getMinecraftVersion().newerOrEquals(MC1_9)) {
            heldItem = e.getPlayer().getInventory().getItemInMainHand(); // You cannot break blocks with items in off hand
        } else {
            heldItem = e.getPlayer().getInventory().getItemInHand();
        }

//        if (!nmsAdapter.isCorrectTool(theBlock.getType(), heldItem.getType())) {
//            return;
//        }

        int fortuneLvl = heldItem.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);

        Location dropLocation = theBlock.getLocation();

        if (heldItem.getEnchantmentLevel(Enchantment.SILK_TOUCH) != 0) {
            Collection<ItemStack> drops = theBlock.getDrops(heldItem);

            if (drops.size() != 1 || drops.stream().findFirst().get().getAmount() != 1) {
                return;
            }

            ItemStack dropStack = drops.stream().findFirst().get();
            dropStack = nmsAdapter.makeGlowingStack(dropStack, emittingLight);

            if (nmsAdapter.getMinecraftVersion().newerOrEquals(MC1_12)) {
                e.setDropItems(false);
            } else {
                e.getBlock().setType(Material.AIR);
            }

            world.dropItemNaturally(dropLocation, dropStack);
        } else {
            if (theBlock.getDrops(heldItem).size() == 0) {
                return;
            }

            ItemStack lui = new ItemStack(lightUpdateItem, 1);

            if (fortuneLvl == 0) {
                world.dropItemNaturally(dropLocation, lui);
            } else {
                // f(x) = 1 - (1 - -0.5) * e^(-0.6 * x)
                double chance = 1d - (1.5) * Math.exp(-0.6 * fortuneLvl);

                for (int i = 0; i < emittingLight; i++) {
                    if (Math.random() <= chance) {
                        world.dropItemNaturally(dropLocation, lui);
                    }
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!configuration.hasReclaim()) {
            return;
        }

        if (e.getItemInHand() == null) {
            return;
        }

        final ItemStack placedStack = e.getItemInHand().clone();
        final int emittingLight = nmsAdapter.getGlowingValue(placedStack);
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

            if (!canNewLightSourceBePlaced(this, e.getBlock().getLocation())) {
                e.setCancelled(true);
                e.setBuild(false);

                nmsAdapter.handleBlockUpdate(e);

                return;
            }

            Bukkit.getScheduler().runTaskLater(this,
                    () -> {
                        LightUpdateResult lightUpdateResult = placeNewLightSource(this, e.getBlock().getLocation(), emittingLight);

                        if (!lightUpdateResult.successful()) {
                            e.getBlock().setType(before);
                            placedStack.setAmount(1);

                            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), placedStack);
                        }
                    }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        handleAreaAffected(e.getBlock().getWorld(), e.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        handleAreaAffected(e.getBlock().getWorld(), e.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        handleAreaAffected(e.getBlock().getWorld(), e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        handleAreaAffected(e.getLocation().getWorld(), e.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        handleBlockAffected(e.getBlock());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        stepSizes.remove(e.getPlayer().getUniqueId());
    }

    // endregion

    private void handleBlockAffected(Block block) {
        WorldLightSourceManager manager = getManager(block.getWorld());

        if (manager == null) {
            return;
        }

        IntPosition position = toIntPosition(block.getLocation());

        PersistentLightSource pls = manager.getPersistentLightSource(block.getLocation());

        if (pls != null) {
            manager.setCustomLuminance(position, 0);
        }
    }

    private void handleAreaAffected(World world, List<Block> blocks) {
        WorldLightSourceManager manager = getManager(world);

        if (manager == null) {
            return;
        }

        for (Block block : blocks) {
            IntPosition position = toIntPosition(block.getLocation());
            PersistentLightSource pls = manager.getPersistentLightSource(block.getLocation());

            if (pls != null) {
                manager.setCustomLuminance(position, 0);
            }
        }
    }


    private boolean isNullOrEmpty(String x) {
        return x == null || x.isEmpty();
    }

    private void unsupportedOperation(String message) {
        getLogger().severe("------------------------------------------------------");
        getLogger().severe(message);
        getLogger().severe("------------------------------------------------------");

        doLoad = false;
    }

    private void loadLightUpdateItem() {
        this.lightUpdateItem = configuration.getLightUpdateItem();
        getLogger().info(String.format("Using \"%s\" as the Light update item.", nmsAdapter.materialToKey(lightUpdateItem)));
    }

    private void loadStepsizeGamemode() {
        this.stepsizeGamemode = configuration.getStepsizeGamemode();
        getLogger().info(String.format("Using, \"%s\" as the Stepsize Gamemode", stepsizeGamemode.name()));
    }
}
