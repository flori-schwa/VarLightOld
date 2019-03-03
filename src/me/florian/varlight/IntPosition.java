package me.florian.varlight;

import net.minecraft.server.v1_13_R2.BlockPosition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class IntPosition {

    private int x, y, z;

    public IntPosition(Location location) {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public IntPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public boolean outOfBounds() {
        return y < 0 || y > 255;
    }

    public Block toBlock(World world) {
        return world.getBlockAt(x, y, z);
    }

    public BlockPosition toBlockPosition() {
        return new BlockPosition(x, y, z);
    }

    public IntPosition getRelative(BlockFace blockFace) {
        return new IntPosition(x + blockFace.getModX(), y + blockFace.getModY(), z + blockFace.getModZ());
    }
}
