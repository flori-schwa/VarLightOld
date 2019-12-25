package me.shawlaf.varlight.command.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CollectionArgumentType<T> implements ArgumentType<Collection<T>> {

    private static final String SEPARATOR = " ";

    private ArgumentType<T> baseArgumentType;
    private String separator;

    public static <V> CollectionArgumentType<V> collection(ArgumentType<V> baseType) {
        return new CollectionArgumentType<>(baseType);
    }

    public static <V> CollectionArgumentType<V> collection(ArgumentType<V> baseType, String separator) {
        return new CollectionArgumentType<>(baseType, separator);
    }

    private CollectionArgumentType(ArgumentType<T> baseArgumentType) {
        this(baseArgumentType, SEPARATOR);
    }

    private CollectionArgumentType(ArgumentType<T> baseArgumentType, String separator) {
        this.baseArgumentType = baseArgumentType;
        this.separator = separator;
    }


    @Override
    public <S> Collection<T> parse(StringReader stringReader) throws CommandSyntaxException {
        List<T> list = new ArrayList<>();

        while (stringReader.canRead()) {
            stringReader.skipWhitespace();
            list.add(baseArgumentType.parse(stringReader));
            stringReader.skipWhitespace();
        }

        return list;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return baseArgumentType.listSuggestions(context, builder);
    }
}
