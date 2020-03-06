package me.shawlaf.varlight.spigot.test.util;

import me.shawlaf.varlight.spigot.util.RegionIterator;
import me.shawlaf.varlight.util.IntPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestRegionIterator {

    @Test
    public void test2x2x2() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = new IntPosition(1, 1, 1);

        RegionIterator regionIterator = new RegionIterator(a, b);

        assertEquals(8, regionIterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), regionIterator.next());
        assertEquals(new IntPosition(0, 0, 1), regionIterator.next());
        assertEquals(new IntPosition(1, 0, 0), regionIterator.next());
        assertEquals(new IntPosition(1, 0, 1), regionIterator.next());
        assertEquals(new IntPosition(0, 1, 0), regionIterator.next());
        assertEquals(new IntPosition(0, 1, 1), regionIterator.next());
        assertEquals(new IntPosition(1, 1, 0), regionIterator.next());
        assertEquals(new IntPosition(1, 1, 1), regionIterator.next());

        assertFalse(regionIterator.hasNext());

        regionIterator = new RegionIterator(b, a);

        assertEquals(8, regionIterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), regionIterator.next());
        assertEquals(new IntPosition(0, 0, 1), regionIterator.next());
        assertEquals(new IntPosition(1, 0, 0), regionIterator.next());
        assertEquals(new IntPosition(1, 0, 1), regionIterator.next());
        assertEquals(new IntPosition(0, 1, 0), regionIterator.next());
        assertEquals(new IntPosition(0, 1, 1), regionIterator.next());
        assertEquals(new IntPosition(1, 1, 0), regionIterator.next());
        assertEquals(new IntPosition(1, 1, 1), regionIterator.next());

        assertFalse(regionIterator.hasNext());

        assertThrows(IndexOutOfBoundsException.class, regionIterator::next);
    }

    @Test
    public void testSingleBlock() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = a;

        RegionIterator iterator = new RegionIterator(a, b);

        assertEquals(1, iterator.getSize());

        assertEquals(a, iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testColumnX() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = new IntPosition(3, 0, 0);

        RegionIterator iterator = new RegionIterator(a, b);

        assertEquals(4, iterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), iterator.next());
        assertEquals(new IntPosition(1, 0, 0), iterator.next());
        assertEquals(new IntPosition(2, 0, 0), iterator.next());
        assertEquals(new IntPosition(3, 0, 0), iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testColumnY() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = new IntPosition(0, 3, 0);

        RegionIterator iterator = new RegionIterator(a, b);

        assertEquals(4, iterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), iterator.next());
        assertEquals(new IntPosition(0, 1, 0), iterator.next());
        assertEquals(new IntPosition(0, 2, 0), iterator.next());
        assertEquals(new IntPosition(0, 3, 0), iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testColumnZ() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = new IntPosition(0, 0, 3);

        RegionIterator iterator = new RegionIterator(a, b);

        assertEquals(4, iterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), iterator.next());
        assertEquals(new IntPosition(0, 0, 1), iterator.next());
        assertEquals(new IntPosition(0, 0, 2), iterator.next());
        assertEquals(new IntPosition(0, 0, 3), iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testPlaneXY() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = new IntPosition(1, 1, 0);

        RegionIterator iterator = new RegionIterator(a, b);

        assertEquals(4, iterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), iterator.next());
        assertEquals(new IntPosition(1, 0, 0), iterator.next());
        assertEquals(new IntPosition(0, 1, 0), iterator.next());
        assertEquals(new IntPosition(1, 1, 0), iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testPlaneXZ() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = new IntPosition(1, 0, 1);

        RegionIterator iterator = new RegionIterator(a, b);

        assertEquals(4, iterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), iterator.next());
        assertEquals(new IntPosition(0, 0, 1), iterator.next());
        assertEquals(new IntPosition(1, 0, 0), iterator.next());
        assertEquals(new IntPosition(1, 0, 1), iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testPlaneYZ() {
        IntPosition a = new IntPosition(0, 0, 0);
        IntPosition b = new IntPosition(0, 1, 1);

        RegionIterator iterator = new RegionIterator(a, b);

        assertEquals(4, iterator.getSize());

        assertEquals(new IntPosition(0, 0, 0), iterator.next());
        assertEquals(new IntPosition(0, 0, 1), iterator.next());
        assertEquals(new IntPosition(0, 1, 0), iterator.next());
        assertEquals(new IntPosition(0, 1, 1), iterator.next());

        assertFalse(iterator.hasNext());
    }

}
