package me.shawlaf.varlight.spigot;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LightUpdateResult {

    private final VarLightPlugin plugin;

    public LightUpdateResult(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    public static LightUpdateResult invalidBlock(VarLightPlugin plugin) {
        return new LightUpdateResult(plugin) {
            @Override
            public @Nullable String getMessage() {
                return null;
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult cancelled(VarLightPlugin plugin) {
        return new LightUpdateResult(plugin) {
            @Override
            public @Nullable String getMessage() {
                return null;
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult zeroReached(VarLightPlugin plugin) {
        return new LightUpdateResult(plugin) {
            @NotNull
            @Override
            public String getMessage() {
                return "Cannot decrease light level below 0.";
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult fifteenReached(VarLightPlugin plugin) {
        return new LightUpdateResult(plugin) {
            @NotNull
            @Override
            public String getMessage() {
                return "Cannot increase light level beyond 15.";
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult updated(VarLightPlugin plugin, int newLightLevel) {
        return new LightUpdateResult(plugin) {
            @Override
            public @Nullable String getMessage() {
                return String.format("Updated Light level to %d", newLightLevel);
            }

            @Override
            public boolean successful() {
                return true;
            }

            @Override
            public int getToLight() {
                return newLightLevel;
            }
        };
    }

    public static LightUpdateResult adjacentLightSource(VarLightPlugin plugin) {
        return new LightUpdateResult(plugin) {
            @NotNull
            @Override
            public String getMessage() {
                return "There is another Light source directly adjacent to this block";
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }

    public static LightUpdateResult varLightNotActive(VarLightPlugin plugin, World world) {
        return new LightUpdateResult(plugin) {
            @Override
            public @Nullable String getMessage() {
                return String.format("Varlight is not active in world \"%s\"", world.getName());
            }

            @Override
            public boolean successful() {
                return false;
            }
        };
    }


    public abstract @Nullable String getMessage();

    public abstract boolean successful();

    public int getToLight() {
        return -1;
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
