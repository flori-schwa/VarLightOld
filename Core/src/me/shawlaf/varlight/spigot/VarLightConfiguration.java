package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class VarLightConfiguration {

    public static final String CONFIG_KEY_VARLIGHT_ITEM = "item";
    public static final String CONFIG_KEY_REQUIRED_PERMISSION = "requiredPermission";
    public static final String CONFIG_KEY_AUTOSAVE = "autosave";
    public static final String CONFIG_KEY_VLDB_DEFLATED = "vldb-deflated";
    public static final String CONFIG_KEY_STEPSIZE_GAMEMODE = "stepsize-gamemode";
    public static final String CONFIG_KEY_VARLIGHT_RECLAIM = "varlight-reclaim";
    public static final String CONFIG_KEY_LOG_DEBUG = "log-debug";
    public static final String CONFIG_KEY_CHECK_UPDATE = "check-update";

    private final VarLightPlugin plugin;

    public VarLightConfiguration(VarLightPlugin plugin) {
        this.plugin = plugin;

        plugin.saveDefaultConfig();

        plugin.getConfig().addDefault(CONFIG_KEY_VARLIGHT_ITEM, plugin.getNmsAdapter().materialToKey(Material.GLOWSTONE_DUST));
        plugin.getConfig().addDefault(CONFIG_KEY_AUTOSAVE, 5);
        plugin.getConfig().addDefault(CONFIG_KEY_REQUIRED_PERMISSION, "");
        plugin.getConfig().addDefault(WorldListType.WHITELIST.configPath, new ArrayList<String>());
        plugin.getConfig().addDefault(WorldListType.BLACKLIST.configPath, new ArrayList<String>());
        plugin.getConfig().addDefault(CONFIG_KEY_VLDB_DEFLATED, true);
        plugin.getConfig().addDefault(CONFIG_KEY_STEPSIZE_GAMEMODE, GameMode.CREATIVE.name());
        plugin.getConfig().addDefault(CONFIG_KEY_VARLIGHT_RECLAIM, true);
        plugin.getConfig().addDefault(CONFIG_KEY_LOG_DEBUG, false);
        plugin.getConfig().addDefault(CONFIG_KEY_CHECK_UPDATE, true);

        plugin.getConfig().options().copyDefaults(true);
    }

    public String getRequiredPermissionNode() {
        return plugin.getConfig().getString(CONFIG_KEY_REQUIRED_PERMISSION);
    }

    public void setRequiredPermissionNode(String permissionNode) {
        plugin.getConfig().set(CONFIG_KEY_REQUIRED_PERMISSION, permissionNode);

        save();
    }

    public Material getLightUpdateItem() {
        String configMaterial = plugin.getConfig().getString(CONFIG_KEY_VARLIGHT_ITEM, "minecraft:glowstone_dust").toLowerCase();

        Material material = plugin.getNmsAdapter().keyToType(configMaterial, MaterialType.ITEM);

        if (material == null) {
            plugin.getLogger().warning(String.format("Could not find a Material with the given name \"%s\", defaulting to \"%s\"", configMaterial, plugin.getNmsAdapter().materialToKey(Material.GLOWSTONE_DUST)));
            return Material.GLOWSTONE_DUST;
        }

        if (plugin.getNmsAdapter().isIllegalLightUpdateItem(material)) {
            plugin.getLogger().warning(String.format("\"%s\" cannot be used as the Light update item. Defaulting to \"%s\"", plugin.getNmsAdapter().materialToKey(material), plugin.getNmsAdapter().materialToKey(Material.GLOWSTONE_DUST)));

            return Material.GLOWSTONE_DUST;
        }

        return material;
    }

    public GameMode getStepsizeGamemode() {
        GameMode def = GameMode.CREATIVE;
        String configGameMode = plugin.getConfig().getString(CONFIG_KEY_STEPSIZE_GAMEMODE, GameMode.CREATIVE.name());
        GameMode gameMode;

        try {
            gameMode = GameMode.valueOf(configGameMode);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(String.format("Could not find a Gamemode with the given name \"%s\", defaulting to \"%s\"", configGameMode, def.name()));
            return def;
        }

        if (gameMode == GameMode.SPECTATOR) {
            plugin.getLogger().warning(String.format("Spectators cannot use VarLight, defaulting the Stepsize gamemode to \"%s\"", def.name()));
            return def;
        }

        return gameMode;
    }

    public boolean isLogDebug() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_LOG_DEBUG, true);
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

    public boolean hasReclaim() {
        return plugin.getConfig().getBoolean(CONFIG_KEY_VARLIGHT_RECLAIM);
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
