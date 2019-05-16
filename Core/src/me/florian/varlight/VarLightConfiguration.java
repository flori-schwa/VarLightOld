package me.florian.varlight;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VarLightConfiguration {

    public static final String CONFIG_KEY_REQUIRED_PERMISSION = "requiredPermission";
    public static final String REQUIRED_PERMISSION_DEFAULT = null;
    public static final String CONFIG_KEY_AUTOSAVE = "autosave";
    public static final int AUTOSAVE_DEFAULT = 5;

    public enum WorldListType {
        WHITELIST("whitelist", "Whitelist"),
        BLACKLIST("blacklist", "Blacklist");

        private String configPath, name;

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

    private VarLightPlugin plugin;

    public VarLightConfiguration(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    public String getRequiredPermissionNode() {
        return plugin.getConfig().getString(CONFIG_KEY_REQUIRED_PERMISSION, REQUIRED_PERMISSION_DEFAULT);
    }

    public void setRequiredPermissionNode(String permissionNode) {
        plugin.getConfig().set(CONFIG_KEY_REQUIRED_PERMISSION, permissionNode);

        save();
    }

    public int getAutosaveInterval() {
        return plugin.getConfig().getInt(CONFIG_KEY_AUTOSAVE, AUTOSAVE_DEFAULT);
    }

    public void setAutosaveInterval(int interval) {
        Preconditions.checkArgument(interval >= 0, "interval must be >= 0");
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

    public List<World> getWorlds(WorldListType type) {
        Objects.requireNonNull(type);

        return plugin.getConfig().getStringList(type.getConfigPath()).stream().map(Bukkit::getWorld).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<World> getVarLightEnabledWorlds() {

        List<World> worlds = getWorlds(WorldListType.WHITELIST);

        if (worlds.isEmpty()) {
            worlds = Bukkit.getWorlds();
        }

        getWorlds(WorldListType.BLACKLIST).forEach(worlds::remove);

        return worlds;
    }

    public void save() {
        plugin.saveConfig();
    }
}
