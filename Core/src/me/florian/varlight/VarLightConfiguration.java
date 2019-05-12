package me.florian.varlight;

import com.google.common.base.Preconditions;

public class VarLightConfiguration {

    public static final String CONFIG_KEY_REQUIRED_PERMISSION = "requiredPermission";
    public static final String REQUIRED_PERMISSION_DEFAULT = null;
    public static final String CONFIG_KEY_AUTOSAVE = "autosave";
    public static final int AUTOSAVE_DEFAULT = 5;

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
        Preconditions.checkArgument(interval > 0, "interval must be > 0");
        plugin.getConfig().set(CONFIG_KEY_AUTOSAVE, interval);
        save();
    }

    public void save() {
        plugin.saveConfig();
    }
}
