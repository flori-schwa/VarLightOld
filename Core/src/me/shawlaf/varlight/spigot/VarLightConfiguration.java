package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VarLightConfiguration {

    public static final String CONFIG_KEY_VARLIGHT_ITEM = "lui";
    public static final String CONFIG_KEY_CONSUME_LUI = "consume-lui";
    public static final String CONFIG_KEY_CHECK_PERMISSION = "check-permission";
    public static final String CONFIG_KEY_AUTOSAVE = "autosave";
    public static final String CONFIG_KEY_NLS_DEFLATED = "nls-deflated";
    public static final String CONFIG_KEY_STEPSIZE_ALLOW_CREATIVE = "stepsize.gamemodes.creative";
    public static final String CONFIG_KEY_STEPSIZE_ALLOW_SURVIVAL = "stepsize.gamemodes.survival";
    public static final String CONFIG_KEY_STEPSIZE_ALLOW_ADVENTURE = "stepsize.gamemodes.adventure";
    public static final String CONFIG_KEY_RECLAIM = "reclaim";
    public static final String CONFIG_KEY_LOG_VERBOSE = "logger.verbose";
    public static final String CONFIG_KEY_LOG_DEBUG = "logger.debug";
    public static final String CONFIG_KEY_CHECK_UPDATE = "check-update";
    public static final String CONFIG_KEY_ENABLE_EXPERIMENTAL_BLOCKS = "experimental-blocks";

    private final VarLightPlugin plugin;

    public VarLightConfiguration(VarLightPlugin plugin) {
        this.plugin = plugin;

        plugin.saveDefaultConfig();

        plugin.getConfig().addDefault(CONFIG_KEY_VARLIGHT_ITEM, plugin.getNmsAdapter().getKey(Material.GLOWSTONE_DUST).toString());
        plugin.getConfig().addDefault(CONFIG_KEY_CONSUME_LUI, true);
        plugin.getConfig().addDefault(CONFIG_KEY_AUTOSAVE, -1); // Persist Light sources on world save
        plugin.getConfig().addDefault(CONFIG_KEY_CHECK_PERMISSION, false);
        plugin.getConfig().addDefault(WorldListType.WHITELIST.configPath, new ArrayList<String>());
        plugin.getConfig().addDefault(WorldListType.BLACKLIST.configPath, new ArrayList<String>());
        plugin.getConfig().addDefault(CONFIG_KEY_NLS_DEFLATED, true);
        plugin.getConfig().addDefault(CONFIG_KEY_STEPSIZE_ALLOW_CREATIVE, true);
        plugin.getConfig().addDefault(CONFIG_KEY_STEPSIZE_ALLOW_SURVIVAL, false);
        plugin.getConfig().addDefault(CONFIG_KEY_STEPSIZE_ALLOW_ADVENTURE, false);
        plugin.getConfig().addDefault(CONFIG_KEY_RECLAIM, true);
        plugin.getConfig().addDefault(CONFIG_KEY_LOG_VERBOSE, false);
        plugin.getConfig().addDefault(CONFIG_KEY_LOG_DEBUG, false);
        plugin.getConfig().addDefault(CONFIG_KEY_CHECK_UPDATE, true);
        plugin.getConfig().addDefault(CONFIG_KEY_ENABLE_EXPERIMENTAL_BLOCKS, false);

        plugin.getConfig().options().copyDefaults(true);
    }

    public boolean isCheckingPermission() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_CHECK_PERMISSION);
    }

    public void setCheckPermission(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_CHECK_PERMISSION, value);

        save();
    }

    public Material getLightUpdateItem() {
        @SuppressWarnings("ConstantConditions") // Cannot be null, because getKey() is NotNull and NamespacedKey.toString() is also NotNull
                String configMaterial = plugin.getConfig().getString(CONFIG_KEY_VARLIGHT_ITEM, plugin.getNmsAdapter().getKey(Material.GLOWSTONE_DUST).toString()).toLowerCase();

        Material material = plugin.getNmsAdapter().keyToType(configMaterial, MaterialType.ITEM);

        if (material == null) {
            plugin.getLogger().warning(String.format("Could not find a Material with the given name \"%s\", defaulting to \"%s\"", configMaterial, plugin.getNmsAdapter().getKey(Material.GLOWSTONE_DUST).toString()));
            return Material.GLOWSTONE_DUST;
        }

        if (plugin.getNmsAdapter().isIllegalLightUpdateItem(material)) {
            plugin.getLogger().warning(String.format("\"%s\" cannot be used as the Light update item. Defaulting to \"%s\"", plugin.getNmsAdapter().getKey(material).toString(), plugin.getNmsAdapter().getKey(Material.GLOWSTONE_DUST).toString()));

            return Material.GLOWSTONE_DUST;
        }

        return material;
    }

    public boolean isConsumeLui() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_CONSUME_LUI);
    }

    public boolean isAllowedStepsizeGamemode(GameMode gameMode) {
        switch (gameMode) {
            case SURVIVAL: {
                return canSurvivalUseStepsize();
            }

            case CREATIVE: {
                return canCreativeUseStepsize();
            }

            case ADVENTURE: {
                return canAdventureUseStepsize();
            }

            case SPECTATOR:
            default: {
                return false;
            }
        }
    }

    public boolean canSurvivalUseStepsize() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_STEPSIZE_ALLOW_SURVIVAL);
    }

    public boolean canCreativeUseStepsize() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_STEPSIZE_ALLOW_CREATIVE);
    }

    public boolean canAdventureUseStepsize() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_STEPSIZE_ALLOW_ADVENTURE);
    }

    public void setConsumeLui(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_CONSUME_LUI, value);
        save();
    }

    public void setCanUseStepsize(GameMode gameMode, boolean value) {
        switch (gameMode) {
            case SURVIVAL: {
                setCanSurvivalUseStepsize(value);
                break;
            }

            case CREATIVE: {
                setCanCreativeUseStepsize(value);
                break;
            }

            case ADVENTURE: {
                setCanAdventureUseStepsize(value);
            }
        }
    }

    public void setCanSurvivalUseStepsize(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_STEPSIZE_ALLOW_SURVIVAL, value);
        save();
    }

    public void setCanCreativeUseStepsize(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_STEPSIZE_ALLOW_CREATIVE, value);
        save();
    }

    public void setCanAdventureUseStepsize(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_STEPSIZE_ALLOW_ADVENTURE, value);
        save();
    }

    public boolean isLogVerbose() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_LOG_VERBOSE);
    }

    public void setLogVerbose(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_LOG_VERBOSE, value);

        save();
    }

    public boolean isLogDebug() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_LOG_DEBUG);
    }

    public void setLogDebug(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_LOG_DEBUG, value);

        save();
    }

    public int getAutosaveInterval() {
        return plugin.getConfig().getInt(CONFIG_KEY_AUTOSAVE);
    }

    public void setAutosaveInterval(int interval) {
        plugin.getConfig().set(CONFIG_KEY_AUTOSAVE, interval);

        save();
    }

    public boolean addWorldToList(World world, WorldListType type) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(type);

        List<World> worlds = getWorlds(type);

        if (worlds.contains(world)) {
            return false;
        }

        worlds.add(world);
        plugin.getConfig().set(type.getConfigPath(), worlds.stream().map(World::getName).collect(Collectors.toList()));

        save();

        return true;
    }

    public boolean removeWorldFromList(World world, WorldListType type) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(type);

        List<World> worlds = getWorlds(type);

        if (!worlds.contains(world)) {
            return false;
        }

        worlds.remove(world);
        plugin.getConfig().set(type.getConfigPath(), worlds.stream().map(World::getName).collect(Collectors.toList()));

        save();

        return true;
    }

    public void clearWorldList(WorldListType type) {
        Objects.requireNonNull(type);

        plugin.getConfig().set(type.getConfigPath(), new ArrayList<>());

        save();
    }

    public boolean isReclaimEnabled() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_RECLAIM);
    }

    public void setReclaim(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_RECLAIM, value);

        save();
    }

    public List<String> getWorldNames(WorldListType type) {
        Objects.requireNonNull(type);
        return plugin.getConfig().getStringList(type.getConfigPath());
    }

    public List<World> getWorlds(WorldListType type) {
        return getWorldNames(type).stream().map(Bukkit::getWorld).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<String> getVarLightEnabledWorldNames() {
        List<String> worlds = getWorldNames(WorldListType.WHITELIST);

        if (worlds.isEmpty()) {
            worlds = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
        }

        getWorldNames(WorldListType.BLACKLIST).forEach(worlds::remove);
        return worlds;
    }

    public List<World> getVarLightEnabledWorlds() {
        return getVarLightEnabledWorldNames().stream().map(Bukkit::getWorld).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean isCheckUpdateEnabled() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_CHECK_UPDATE);
    }

    public void setCheckUpdate(boolean value) {
        plugin.getConfig().set(CONFIG_KEY_CHECK_UPDATE, value);

        save();
    }

    public boolean isAllowExperimentalBlocks() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_ENABLE_EXPERIMENTAL_BLOCKS);
    }

    public void save() {
        plugin.saveConfig();
    }

    public enum WorldListType {
        WHITELIST("whitelist", "Whitelist"),
        BLACKLIST("blacklist", "Blacklist");

        private final String configPath;
        private final String name;

        WorldListType(String configPath, String name) {
            this.configPath = configPath;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getConfigPath() {
            return configPath;
        }
    }
}
