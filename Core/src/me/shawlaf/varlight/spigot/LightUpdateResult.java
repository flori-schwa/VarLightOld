package me.shawlaf.varlight.spigot;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LightUpdateResult {

    private final VarLightPlugin plugin;
    private final int fromLight, toLight;

    public LightUpdateResult(VarLightPlugin plugin, int fromLight, int toLight) {
        this.plugin = plugin;
        this.fromLight = fromLight;
        this.toLight = toLight;
    }

    public static LightUpdateResult invalidBlock(VarLightPlugin plugin, int fromLight, int toLight) {
        return new LightUpdateResult(plugin, fromLight, toLight) {
            @Override
            public @Nullable String getMessage() {
                return null;
            }

            @Override
            public @NotNull NamespacedKey getDebugMessage() {
                return new NamespacedKey(plugin, "invalid_block");
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult cancelled(VarLightPlugin plugin, int fromLight, int toLight) {
        return new LightUpdateResult(plugin, fromLight, toLight) {
            @Override
            public @Nullable String getMessage() {
                return null;
            }

            @Override
            public @NotNull NamespacedKey getDebugMessage() {
                return new NamespacedKey(plugin, "cancelled");
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult zeroReached(VarLightPlugin plugin, int fromLight, int toLight) {
        return new LightUpdateResult(plugin, fromLight, toLight) {
            @NotNull
            @Override
            public String getMessage() {
                return "Cannot decrease light level below 0.";
            }

            @Override
            public @NotNull NamespacedKey getDebugMessage() {
                return new NamespacedKey(plugin, "zero_reached");
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult fifteenReached(VarLightPlugin plugin, int fromLight, int toLight) {
        return new LightUpdateResult(plugin, fromLight, toLight) {
            @NotNull
            @Override
            public String getMessage() {
                return "Cannot increase light level beyond 15.";
            }

            @Override
            public @NotNull NamespacedKey getDebugMessage() {
                return new NamespacedKey(plugin, "fifteen_reached");
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult updated(VarLightPlugin plugin, int fromLight, int toLight) {
        return new LightUpdateResult(plugin, fromLight, toLight) {
            @Override
            public @Nullable String getMessage() {
                return String.format("Updated Light level to %d", toLight);
            }

            @Override
            public @NotNull NamespacedKey getDebugMessage() {
                return new NamespacedKey(plugin, "updated");
            }

            @Override
            public boolean successful() {
                return true;
            }
        };
    }

    public static LightUpdateResult varLightNotActive(VarLightPlugin plugin, World world, int fromLight, int toLight) {
        return new LightUpdateResult(plugin, fromLight, toLight) {
            @Override
            public @Nullable String getMessage() {
                return String.format("Varlight is not active in world \"%s\"", world.getName());
            }

            @Override
            public @NotNull NamespacedKey getDebugMessage() {
                return new NamespacedKey(plugin, "varlight_not_active");
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }


    public abstract @Nullable String getMessage();

    public abstract @NotNull NamespacedKey getDebugMessage();

    public abstract boolean successful();

    public int getToLight() {
        return toLight;
    }

    public int getFromLight() {
        return fromLight;
    }

    public void displayMessage(CommandSender commandSender) {

        String message = getMessage();

        if (message == null) {
            return;
        }

        if (commandSender instanceof Player) {
            plugin.getNmsAdapter().sendActionBarMessage((Player) commandSender, message);
        } else {
            commandSender.sendMessage(message);
        }
    }

}
