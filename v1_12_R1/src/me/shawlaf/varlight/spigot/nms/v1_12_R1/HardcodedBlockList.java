package me.shawlaf.varlight.spigot.nms.v1_12_R1;

import org.bukkit.Material;

public class HardcodedBlockList {

    public static final HardcodedBlockList ALLOWED_BLOCKS;
    public static final HardcodedBlockList EXPERIMENTAL_BLOCKS;

    public boolean contains(Material material) {
        return true; // TODO Implement
    }

    static {
        ALLOWED_BLOCKS = new HardcodedBlockList();
        EXPERIMENTAL_BLOCKS = new HardcodedBlockList();
    }
}
