package me.shawlaf.varlight.spigot.prompt;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ChatPromptManager {

    private ChatPrompt consolePrompt;
    private final Map<UUID, ChatPrompt> activePrompts = new HashMap<>();
    private final VarLightPlugin plugin;

    public ChatPromptManager(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasActivePrompt(Entity entity) {
        if (!activePrompts.containsKey(entity.getUniqueId())) {
            return false;
        }

        ChatPrompt prompt = activePrompts.get(entity.getUniqueId());

        if (prompt.isActive()) {
            return true;
        }

        activePrompts.remove(entity.getUniqueId());
        return false;
    }

    public boolean startPrompt(@NotNull CommandSender source, @NotNull BaseComponent[] message, @NotNull Runnable onConfirm, long timeout, @NotNull TimeUnit timeUnit) {
        ChatPrompt prompt;

        if (source instanceof Entity) {
            prompt = activePrompts.get(((Entity) source).getUniqueId());
        } else if (source instanceof ConsoleCommandSender) {
            prompt = consolePrompt;
        } else {
            source.sendMessage("Block command sender not supported.");
            return false;
        }

        if (prompt != null && prompt.isActive()) {
            source.sendMessage(ChatColor.RED + "You already have an active prompt running:");
            prompt.sendMessage(source);
            return false;
        }

        ChatPrompt chatPrompt = new ChatPrompt(plugin, message, onConfirm);

        chatPrompt.start(source, timeout, timeUnit);
        source.sendMessage(ChatColor.RED + "Type /varlight prompt confirm/cancel to confirm/cancel.");

        if (source instanceof Entity) {
            activePrompts.put(((Entity) source).getUniqueId(), chatPrompt);
        } else {
            consolePrompt = chatPrompt;
        }

        return true;
    }

    private ChatPrompt checkActivePrompt(CommandSender source) {
        ChatPrompt prompt;

        if (source instanceof Entity) {
            prompt = activePrompts.get(((Entity) source).getUniqueId());
        } else if (source instanceof ConsoleCommandSender) {
            prompt = consolePrompt;
        } else {
            source.sendMessage("Block command sender not supported.");
            return null;
        }

        if (prompt == null || !prompt.isActive()) {
            source.sendMessage(ChatColor.RED + "You currently don't have an active prompt running.");
            return null;
        }

        return prompt;
    }

    public void confirmPrompt(CommandSender source) {
        ChatPrompt prompt = checkActivePrompt(source);

        if (prompt != null) {
            prompt.confirm();
        }
    }

    public void cancelPrompt(CommandSender source) {
        ChatPrompt prompt = checkActivePrompt(source);

        if (prompt != null) {
            prompt.cancel();
        }
    }
}
