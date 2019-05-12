package me.florian.varlight.command;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArgumentStream {
    public final int length;
    private String[] arguments;
    private int position = 0;

    public ArgumentStream(String[] args) {
        this.length = args.length;
        this.arguments = args;
    }

    public String get(int position) {
        return arguments[position];
    }

    public boolean hasNext() {
        return position < length;
    }

    public String peek() {
        return arguments[position];
    }

    public String get() {
        return arguments[position++];
    }

    public <P> P parseNext(Function<String, P> function) {
        return function.apply(get());
    }

    public String join() {
        return join(" ");
    }

    public Stream<String> streamRemaining() {
        Stream.Builder<String> streamBuilder = Stream.builder();

        while (hasNext()) {
            streamBuilder.accept(get());
        }

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
}