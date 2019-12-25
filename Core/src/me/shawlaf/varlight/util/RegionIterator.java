package me.shawlaf.varlight.util;

import org.bukkit.World;

import java.util.Iterator;

public class RegionIterator implements Iterator<IntPosition> {

    public final IntPosition pos1, pos2;
    private final int modX, modY, modZ;
    private final boolean isSingleBlock;
    private boolean returnedSingleBlock = false;

    private int x, y, z;

    public RegionIterator(IntPosition pos1, IntPosition pos2) {
        if (pos1.compareTo(pos2) < 0) {
            // pos1 closer to origin

            this.pos1 = pos1;
            this.pos2 = pos2;
        } else {
            // pos2 closer to origin

            this.pos1 = pos2;
            this.pos2 = pos1;
        }

        this.modX = binaryStep(this.pos2.x - this.pos1.x);
        this.modY = binaryStep(this.pos2.y - this.pos1.y);
        this.modZ = binaryStep(this.pos2.z - this.pos1.z);

        this.isSingleBlock = pos1.equals(pos2);

        reset();
    }

    private static int binaryStep(int x) {
        return Integer.compare(x, 0);
    }

    private boolean xInRange(int x) {
        if (pos1.x < pos2.x) {
            return x >= pos1.x && x <= pos2.x;
        } else {
            return x >= pos2.x && x <= pos1.x;
        }
    }

    private boolean yInRange(int y) {
        if (pos1.y < pos2.y) {
            return y >= pos1.y && y <= pos2.y;
        } else {
            return y >= pos2.y && y <= pos1.y;
        }
    }

    private boolean zInRange(int z) {
        if (pos1.z < pos2.z) {
            return z >= pos1.z && z <= pos2.z;
        } else {
            return z >= pos2.z && z <= pos1.z;
        }
    }

    public boolean isRegionLoaded(World world) {
        reset();
        IntPosition next;

        while (hasNext()) {
            next = next();

            if (!world.isChunkLoaded(next.getChunkX(), next.getChunkZ())) {
                return false;
            }
        }

        return true;
    }

    public void reset() {
        this.x = this.pos1.x;
        this.y = this.pos1.y;
        this.z = this.pos1.z - modZ; // to include the first block in the region

        if (isSingleBlock) {
            returnedSingleBlock = false;
        }
    }

    @Override
    public boolean hasNext() {
        return isSingleBlock ? !returnedSingleBlock : !(this.x == pos2.x && this.y == pos2.y && this.z == pos2.z);
    }

    @Override
    public IntPosition next() {
        if (isSingleBlock && !returnedSingleBlock) {
            returnedSingleBlock = true;
            return pos1;
        } else if (isSingleBlock) {
            throw new IndexOutOfBoundsException("Already Iterated over entire region!");
        }

        this.z += modZ;

        if (zInRange(z)) {
            return new IntPosition(x, y, z);
        }

        this.z = pos1.z;
        this.x += modX;

        if (xInRange(x)) {
            return new IntPosition(x, y, z);
        }

        this.x = pos1.x;
        this.y += modY;

        if (yInRange(y)) {
            return new IntPosition(x, y, z);
        }

        throw new IndexOutOfBoundsException("Already Iterated over entire region!");
    }
}
