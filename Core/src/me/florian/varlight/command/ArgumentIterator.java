package me.florian.varlight.command;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArgumentIterator implements Iterator<String> {
    public final int length;
    private String[] arguments;
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

    @Override
    public String next() {
        return arguments[position++];
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
}