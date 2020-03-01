package me.shawlaf.varlight.spigot.command.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import org.bukkit.Material;

import java.util.concurrent.CompletableFuture;

public class MinecraftTypeArgumentType implements ArgumentType<Material> {

    private final VarLightPlugin plugin;
    private final MaterialType type;

    private MinecraftTypeArgumentType(VarLightPlugin plugin, MaterialType type) {
        this.plugin = plugin;
        this.type = type;
    }

    public static MinecraftTypeArgumentType minecraftType(VarLightPlugin plugin, MaterialType type) {
        return new MinecraftTypeArgumentType(plugin, type);
    }

    private boolean isLegalNamespaceChar(char c) {
        if (c >= '0' && c <= '9') {
            return true;
        }

        if (c >= 'a' && c <= 'z') {
            return true;
        }

        return c == '-' || c == '_';
    }

    private boolean isLegalKeyChar(char c) {
        return isLegalNamespaceChar(c) || c == '.' || c == '/';
    }

    private String readKey(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();

        while (reader.canRead() && (reader.peek() == ':' || isLegalKeyChar(reader.peek()))) {
            reader.skip();
        }

        if (reader.canRead() && reader.peek() != ' ') {
            throw new SimpleCommandExceptionType(() -> "Expected End of string or whitespace!").create();
        }

        String fullKey = reader.getString().substring(start, reader.getCursor());

        if (fullKey.contains(":")) {
            // Key is namespaced -> we need to check the namespace.

            // The key is guaranteed to be valid, because we only read valid key characters.
            // And the set of valid namespace character is a sub-set of all valid key characters.
            String namespace = fullKey.split(":")[0];

            for (char c : namespace.toCharArray()) {
                if (!isLegalNamespaceChar(c)) {
                    throw new SimpleCommandExceptionType(() -> "Illegal character \"" + c + "\" in namespace!").create();
                }
            }
        }

        return fullKey;
    }

    @Override
    public Material parse(StringReader stringReader) throws CommandSyntaxException {
        return plugin.getNmsAdapter().keyToType(readKey(stringReader), type);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        plugin.getNmsAdapter().getTypes(type).forEach(builder::suggest);

        return builder.buildFuture();
    }
}
