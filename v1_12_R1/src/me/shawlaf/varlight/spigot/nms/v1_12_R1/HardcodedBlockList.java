package me.shawlaf.varlight.spigot.nms.v1_12_R1;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HardcodedBlockList {

    private final Set<Material> blockSet = new HashSet<>();

    public static final HardcodedBlockList ILLEGAL_BLOCKS;
    public static final HardcodedBlockList EXPERIMENTAL_BLOCKS;

    public HardcodedBlockList(Material... types) {
        blockSet.addAll(Arrays.asList(types));
    }

    public boolean contains(Material material) {
        return blockSet.contains(material);
    }

    static {
        ILLEGAL_BLOCKS = new HardcodedBlockList(
                Material.AIR,
                Material.SAPLING,
                Material.LONG_GRASS,
                Material.DEAD_BUSH


        );
        EXPERIMENTAL_BLOCKS = new HardcodedBlockList();
    }
}
