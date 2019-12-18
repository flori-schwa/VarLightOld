package me.shawlaf.varlight.command_old;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated
public class ArgumentIterator implements Iterator<String> {
    public final int length;
    private final String[] arguments;
    private int position = 0;

    public ArgumentIterator(String[] args) {
        this.length = args.length;
        this.arguments = args;
    }

    public String next(int position) {
        return arguments[position];
    }

    @Override
    public boolean hasNext() {
        return position < length;
    }

    public String peek() {
        return arguments[position];
    }

    public String get(int index) {
        return arguments[index];
    }

    @Override
    public String next() {
        return arguments[position++];
    }

    public int nextInt() {
        return parseNext(Integer::parseInt);
    }

    public boolean hasParameters(int required) {
        return (length - position) >= required;
    }

    public String previous() {
        return arguments[position - 1];
    }

    public <P> P parseNext(Function<String, P> function) {
        return function.apply(next());
    }

    public String join() {
        return join(" ");
    }

    public Stream<String> streamRemaining() {
        Stream.Builder<String> streamBuilder = Stream.builder();
        forEachRemaining(streamBuilder);
        return streamBuilder.build();
    }

    public String join(String delimiter) {
        return streamRemaining().collect(Collectors.joining(delimiter));
    }

    public int getPosition() {
        return position;
    }

    public String jumpTo(int position) {
        return arguments[this.position = position];
    }

    @Override
    public String toString() {
        return "ArgumentIterator{" +
                "length=" + length +
                ", arguments=" + Arrays.toString(arguments) +
                ", position=" + position +
                '}';
    }
}