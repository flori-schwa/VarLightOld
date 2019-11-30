package me.shawlaf.varlight.util;

import java.util.Arrays;
import java.util.Objects;

public class NumericMajorMinorVersion implements Comparable<NumericMajorMinorVersion> {

    private final int[] versions;

    public NumericMajorMinorVersion(String version) {
        int expected = occurencesOfDot(version) + 1;
        String[] parts = version.split("\\.");

        if (expected != parts.length) {
            throw forVersionString(version, null);
        }

        versions = new int[parts.length];

        try {
            for (int i = 0; i < versions.length; i++) {
                versions[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            throw forVersionString(version, e);
        }
    }

    public static boolean isValid(String input) {
        try {
            new NumericMajorMinorVersion(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static IllegalArgumentException forVersionString(String input, Throwable cause) {
        return new IllegalArgumentException(String.format("Illegal version string %s", input), cause);
    }

    private static int occurencesOfDot(String in) {
        int count = 0;

        for (char c : in.toCharArray()) {
            if (c == '.') {
                count++;
            }
        }

        return count;
    }

    public int length() {
        return versions.length;
    }

    public boolean newerThan(NumericMajorMinorVersion other) {
        Objects.requireNonNull(other);

        return compareTo(other) > 0;
    }

    public boolean newerOrEquals(NumericMajorMinorVersion other) {
        return newerThan(other) || equals(other);
    }

    public boolean olderThan(NumericMajorMinorVersion other) {
        Objects.requireNonNull(other);

        return compareTo(other) < 0;
    }

    public boolean olderOrEquals(NumericMajorMinorVersion other) {
        return olderThan(other) || equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumericMajorMinorVersion that = (NumericMajorMinorVersion) o;
        return Arrays.equals(versions, that.versions);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(versions);
    }

    @Override
    public int compareTo(NumericMajorMinorVersion o) {
        Objects.requireNonNull(o);

        int parts = Math.min(length(), o.length());

        for (int i = 0; i < parts; i++) {
            int diff = Integer.compare(versions[i], o.versions[i]);

            if (diff != 0) {
                return diff;
            }
        }

        return Integer.compare(length(), o.length());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(String.valueOf(versions[0]));

        for (int i = 1; i < versions.length; i++) {
            builder.append('.').append(versions[i]);
        }

        return builder.toString();
    }
}
