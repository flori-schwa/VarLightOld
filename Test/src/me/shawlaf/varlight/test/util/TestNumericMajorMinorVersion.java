package me.shawlaf.varlight.test.util;

import me.shawlaf.varlight.util.NumericMajorMinorVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class TestNumericMajorMinorVersion {

    @Test
    public void testParse() {
        String goodInput = "1.0.5";
        String badInput = "1.5.3a", badInput2 = "1.5.";

        Assertions.assertEquals(goodInput, new NumericMajorMinorVersion(goodInput).toString());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NumericMajorMinorVersion(badInput));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NumericMajorMinorVersion(badInput2));
    }

    @Test
    public void testLength() {
        String[] inputs = {
                "1.0",
                "1.3.2",
                "1.5.7.9",
                "1"
        };

        Assertions.assertEquals(2, new NumericMajorMinorVersion(inputs[0]).length());
        Assertions.assertEquals(3, new NumericMajorMinorVersion(inputs[1]).length());
        Assertions.assertEquals(4, new NumericMajorMinorVersion(inputs[2]).length());
        Assertions.assertEquals(1, new NumericMajorMinorVersion(inputs[3]).length());
    }

    @Test
    public void testEquals() {
        NumericMajorMinorVersion a = new NumericMajorMinorVersion("0.0.1");
        NumericMajorMinorVersion b = new NumericMajorMinorVersion("0.0.1.3");
        NumericMajorMinorVersion c = new NumericMajorMinorVersion("0.0.1");

        Assertions.assertEquals(a, a);
        Assertions.assertEquals(a, c);
        Assertions.assertNotEquals(a, null);
        Assertions.assertNotEquals(a, b);
    }

    @Test
    public void testHashcode() {
        NumericMajorMinorVersion a = new NumericMajorMinorVersion("1.0.0");
        NumericMajorMinorVersion b = new NumericMajorMinorVersion("2.0.0");
        NumericMajorMinorVersion c = new NumericMajorMinorVersion("1.0");
        NumericMajorMinorVersion d = new NumericMajorMinorVersion("2.0");

        Assertions.assertEquals(a.hashCode(), a.hashCode());
        Assertions.assertNotEquals(a.hashCode(), c.hashCode());

        Assertions.assertEquals(b.hashCode(), b.hashCode());
        Assertions.assertNotEquals(b.hashCode(), d.hashCode());

        Assertions.assertNotEquals(a.hashCode(), Objects.hashCode(null));
    }

    @Test
    public void testCompare() {
        NumericMajorMinorVersion a = new NumericMajorMinorVersion("2.5.8");
        NumericMajorMinorVersion b = new NumericMajorMinorVersion("2.5.7");
        NumericMajorMinorVersion c = new NumericMajorMinorVersion("2.5.9");
        NumericMajorMinorVersion d = new NumericMajorMinorVersion("2.5.8");

        Assertions.assertEquals(0, a.compareTo(d));
        Assertions.assertEquals(1, a.compareTo(b));
        Assertions.assertEquals(-1, b.compareTo(a));
        Assertions.assertEquals(-1, a.compareTo(c));
        Assertions.assertEquals(1, c.compareTo(a));

        Assertions.assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    @Test
    public void testNewerThan() {
        NumericMajorMinorVersion a = new NumericMajorMinorVersion("2.5.8");
        NumericMajorMinorVersion b = new NumericMajorMinorVersion("2.5.7");
        NumericMajorMinorVersion c = new NumericMajorMinorVersion("2.5.9");
        NumericMajorMinorVersion d = new NumericMajorMinorVersion("2.5.8");

        Assertions.assertTrue(a.newerThan(b));
        Assertions.assertTrue(c.newerThan(a));
        Assertions.assertFalse(a.newerThan(d));
        Assertions.assertFalse(b.newerThan(a));
        Assertions.assertThrows(NullPointerException.class, () -> a.newerThan(null));
    }

    @Test
    public void testOlderThan() {
        NumericMajorMinorVersion a = new NumericMajorMinorVersion("2.5.8");
        NumericMajorMinorVersion b = new NumericMajorMinorVersion("2.5.7");
        NumericMajorMinorVersion c = new NumericMajorMinorVersion("2.5.9");
        NumericMajorMinorVersion d = new NumericMajorMinorVersion("2.5.8");

        Assertions.assertTrue(a.olderThan(c));
        Assertions.assertTrue(b.olderThan(a));
        Assertions.assertFalse(c.olderThan(a));
        Assertions.assertFalse(a.olderThan(d));

        Assertions.assertThrows(NullPointerException.class, () -> a.olderThan(null));
    }


}
