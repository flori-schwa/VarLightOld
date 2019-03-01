package me.florian.varlight;

public final class IntPosition {

    private int x;
    private int y;
    private int z;

    public IntPosition(long encoded) {
        this.x = (int) (encoded >> 38);
        this.y = (int) ((encoded >> 26) & 0xFFF);
        this.z = (int) (encoded << 38 >> 38);
    }

    public IntPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getChunkX() {
        return x / 16;
    }

    public int getChunkZ() {
        return z / 16;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public long encode() {
        return ((x & 0x3FFFFFF) << 38) | ((y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IntPosition) {
            IntPosition other = (IntPosition) obj;

            return x == other.x && y == other.y && z == other.z;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;

        return result;
    }
}
