package me.florian.varlight.nms;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.util.NumericMajorMinorVersion;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface INmsAdapter {

    default void onLoad(VarLightPlugin plugin, boolean use) {

    }

    default void onEnable(VarLightPlugin plugin, boolean use) {

    }

    default void onDisable(boolean wasUsed) {

    }

    boolean isBlockTransparent(Block block);

    void recalculateBlockLight(Location at);

    void updateBlockLight(Location at, int lightLevel);

    int getEmittingLightLevel(Block block);

    void sendChunkUpdates(Chunk chunk, int mask);

    default void sendChunkUpdates(Chunk chunk) {
        sendChunkUpdates(chunk, (1 << 16) - 1);
    }

    boolean isValidBlock(Block block);

    void sendActionBarMessage(Player player, String message);

    void setCooldown(Player player, Material material, int ticks);

    boolean hasCooldown(Player player, Material material);

    String getNumericMinecraftVersion();

    default NumericMajorMinorVersion getMinecraftVersion() {
        return new NumericMajorMinorVersion(getNumericMinecraftVersion());
    }

    default void suggestCommand(Player player, String command) {
        player.spigot().sendMessage(
                new ComponentBuilder(String.format("Click to here to run command %s", command))
                        .color(ChatColor.GRAY)
                        .italic(true)
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))

                        .event(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("You can thank MC-70317").color(ChatColor.GRAY).italic(true).create())
                        )
                        .create()
        );
    }
}
