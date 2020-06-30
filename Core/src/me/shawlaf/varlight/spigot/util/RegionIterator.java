package me.shawlaf.varlight.spigot.util;

import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RegionIterator implements Iterator<IntPosition> {

    public final IntPosition pos1, pos2;
    private final int modX, modY, modZ;

    private boolean done = false;

    private int nextX, nextY, nextZ;

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

        reset();
    }

    private static int binaryStep(int x) {
        return Integer.compare(x, 0);
    }

    public int getSize() {
        int xSize = Math.abs(pos2.x - pos1.x) + 1;
        int ySize = Math.abs(pos2.y - pos1.y) + 1;
        int zSize = Math.abs(pos2.z - pos1.z) + 1;

        return xSize * ySize * zSize;
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
        Iterator<ChunkCoords> chunkIterator = iterateChunks();

        while (chunkIterator.hasNext()) {
            ChunkCoords coords = chunkIterator.next();

            if (!world.isChunkLoaded(coords.x, coords.z)) {
                return false;
            }
        }

        return true;
    }

    public Iterator<ChunkCoords> iterateChunks() {
        final int startChunkX = pos1.getChunkX();
        final int startChunkZ = pos1.getChunkZ();
        final int endChunkX = pos2.getChunkX();
        final int endChunkZ = pos2.getChunkZ();

        final int modX = binaryStep(endChunkX - startChunkX);
        final int modZ = binaryStep(endChunkZ - startChunkZ);

        return new Iterator<ChunkCoords>() {
            int x = startChunkX;
            int z = startChunkZ;

            boolean done = false;

            @Override
            public boolean hasNext() {
                return !done;
            }

            private boolean zInRange(int z) {
                if (startChunkZ < endChunkZ) {
                    return z >= startChunkZ && z <= endChunkZ;
                } else {
                    return z >= endChunkZ && z <= startChunkZ;
                }
            }

            private boolean xInRange(int x) {
                if (startChunkX < endChunkX) {
                    return x >= startChunkX && x <= endChunkX;
                } else {
                    return x >= endChunkX && x <= startChunkX;
                }
            }

            private boolean incrZ() {
                z += modZ;
                return zInRange(z);
            }

            private boolean incrX() {
                x += modX;
                return xInRange(x);
            }

            private void incrNext() {
                if (modZ != 0) {
                    if (!incrZ()) {
                        nextZ = pos1.z;
                    } else {
                        return;
                    }
                }

                if (modX != 0) {
                    if (incrX()) {
                        return;
                    }
                }

                done = true;
            }

            @Override
            public ChunkCoords next() {
                if (done) {
                    throw new IndexOutOfBoundsException("Already iterated over chunks");
                }

                ChunkCoords next = new ChunkCoords(x, z);
                incrNext();

                return next;
            }
        };
    }

    public Set<ChunkCoords> collectChunks() {
        Set<ChunkCoords> chunks = new HashSet<>();
        Iterator<ChunkCoords> chunkIterator = iterateChunks();

        while (chunkIterator.hasNext()) {
            chunks.add(chunkIterator.next());
        }

        return chunks;
    }

    public void reset() {
        this.nextX = this.pos1.x;
        this.nextY = this.pos1.y;
        this.nextZ = this.pos1.z;

        done = false;
    }

    @Override
    public boolean hasNext() {
        return !done;
//        return isSingleBlock ? !returnedSingleBlock : !(this.nextX == pos2.x && this.nextY == pos2.y && this.nextZ == pos2.z);
    }

    private boolean incrementZ() {
        nextZ += modZ;
        return zInRange(nextZ);
    }

    private boolean incrementX() {
        nextX += modX;
        return xInRange(nextX);
    }

    private boolean incrementY() {
        nextY += modY;
        return yInRange(nextY);
    }

    private void incrementNext() {
        if (modZ != 0) {
            if (!incrementZ()) {
                nextZ = pos1.z;
            } else {
                return;
            }
        }

        if (modX != 0) {
            if (!incrementX()) {
                nextX = pos1.x;
            } else {
                return;
            }
        }

        if (modY != 0) {
            if (incrementY()) {
                return;
            }
        }

        done = true;
    }

    @Override
    public IntPosition next() {
        if (done) {
            throw new IndexOutOfBoundsException("Already Iterated over entire region!");
        }

        IntPosition next = new IntPosition(nextX, nextY, nextZ);
        incrementNext();

        return next;
    }
}
