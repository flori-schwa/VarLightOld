package me.shawlaf.varlight.spigot.executor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Ticks {

    public final long ticks;

    private Ticks(long ticks) {
        this.ticks = ticks;
    }

    public static Ticks of(long nTicks) {
        return new Ticks(nTicks);
    }

    public static Ticks of(long n, TimeUnit timeUnit) {
        return new Ticks(timeUnit.toSeconds(n) * 20L);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticks ticks1 = (Ticks) o;
        return ticks == ticks1.ticks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticks);
    }
}
