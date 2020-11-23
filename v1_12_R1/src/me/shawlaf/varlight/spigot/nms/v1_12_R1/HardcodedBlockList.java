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
                Material.DEAD_BUSH,
                Material.WATER_LILY,
                Material.DOUBLE_PLANT,
                Material.CHORUS_PLANT,
                Material.CHORUS_FLOWER,

                Material.WHITE_SHULKER_BOX,
                Material.ORANGE_SHULKER_BOX,
                Material.MAGENTA_SHULKER_BOX,
                Material.LIGHT_BLUE_SHULKER_BOX,
                Material.YELLOW_SHULKER_BOX,
                Material.LIME_SHULKER_BOX,
                Material.PINK_SHULKER_BOX,
                Material.GRAY_GLAZED_TERRACOTTA,
                Material.SILVER_SHULKER_BOX,
                Material.CYAN_SHULKER_BOX,
                Material.PURPLE_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX,
                Material.BROWN_SHULKER_BOX,
                Material.GREEN_SHULKER_BOX,
                Material.RED_SHULKER_BOX,
                Material.BLACK_SHULKER_BOX,

                Material.SIGN_POST,
                Material.WALL_SIGN,
                Material.BED_BLOCK,
                Material.FLOWER_POT,
                Material.STANDING_BANNER,
                Material.WALL_BANNER,
                Material.PISTON_EXTENSION,
                Material.TNT,
                Material.REDSTONE_LAMP_OFF,
                Material.REDSTONE_LAMP_ON,
                Material.TRIPWIRE_HOOK,
                Material.WOOD_BUTTON,
                Material.STONE_BUTTON,
                Material.WOOD_PLATE,
                Material.STONE_PLATE,
                Material.GOLD_PLATE,
                Material.IRON_PLATE,
                Material.WOODEN_DOOR,
                Material.BIRCH_DOOR,
                Material.SPRUCE_DOOR,
                Material.JUNGLE_DOOR,
                Material.ACACIA_DOOR,
                Material.DARK_OAK_DOOR,
                Material.IRON_DOOR_BLOCK,
                Material.REDSTONE_WIRE,
                Material.DIODE_BLOCK_OFF,
                Material.DIODE_BLOCK_ON,
                Material.REDSTONE_COMPARATOR_OFF,
                Material.REDSTONE_COMPARATOR_ON,
                Material.POWERED_RAIL,
                Material.DETECTOR_RAIL,
                Material.RAILS,
                Material.ACTIVATOR_RAIL,
                Material.WATER,
                Material.STATIONARY_WATER,
                Material.LAVA,
                Material.STATIONARY_LAVA,
                Material.SUGAR_CANE_BLOCK,
                Material.CROPS,
                Material.POTATO,
                Material.CARROT,
                Material.BEETROOT_BLOCK,
                Material.PUMPKIN_STEM,
                Material.MELON_STEM
        );

        EXPERIMENTAL_BLOCKS = new HardcodedBlockList(); // Empty, will be removed
    }
}
