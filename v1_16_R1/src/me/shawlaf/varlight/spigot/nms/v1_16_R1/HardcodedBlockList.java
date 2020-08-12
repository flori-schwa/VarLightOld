package me.shawlaf.varlight.spigot.nms.v1_16_R1;

import net.minecraft.server.v1_16_R1.MinecraftKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.HashSet;
import java.util.Set;

public class HardcodedBlockList {

    public static final HardcodedBlockList ALLOWED_BLOCKS;
    public static final HardcodedBlockList EXPERIMENTAL_BLOCKS;

    private final Set<NamespacedKey> keys;
    private final Set<NamespacedKey> tags;

    @SuppressWarnings("deprecation")
    public HardcodedBlockList(String[] keys) {
        this.keys = new HashSet<>();
        this.tags = new HashSet<>();

        for (String key : keys) {

            if (key.startsWith("#")) { // Entry is a tag
                key = key.substring(1);
                MinecraftKey mcKey = MinecraftKey.a(key);

                this.tags.add(new NamespacedKey(mcKey.getNamespace(), mcKey.getKey()));
                continue;
            }

            MinecraftKey mcKey = MinecraftKey.a(key);

            this.keys.add(new NamespacedKey(mcKey.getNamespace(), mcKey.getKey()));
        }
    }

    public boolean contains(Material material) {
        if (keys.contains(material.getKey())) {
            return true;
        }

        for (NamespacedKey tagKey : tags) {
            if (Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class).isTagged(material)) {
                keys.add(material.getKey()); // Add the key to list of allowed keys for faster future lookups
                return true;
            }
        }

        return false;
    }

    static {
        ALLOWED_BLOCKS = new HardcodedBlockList(new String[]{
                "#minecraft:beehives",
                "#minecraft:coral_blocks",
                "#minecraft:leaves",
                "#minecraft:logs",
                "#minecraft:planks",
                "#minecraft:sand",
                "#minecraft:slabs",
                "#minecraft:stairs",
                "#minecraft:stone_bricks",
                "#minecraft:wool",
                "minecraft:ancient_debris",
                "minecraft:andesite",
                "minecraft:basalt",
                "minecraft:bedrock",
                "minecraft:black_concrete",
                "minecraft:black_concrete_powder",
                "minecraft:black_glazed_terracotta",
                "minecraft:black_terracotta",
                "minecraft:blackstone",
                "minecraft:blue_concrete",
                "minecraft:blue_concrete_powder",
                "minecraft:blue_glazed_terracotta",
                "minecraft:blue_ice",
                "minecraft:blue_terracotta",
                "minecraft:bone_block",
                "minecraft:bookshelf",
                "minecraft:bricks",
                "minecraft:brown_concrete",
                "minecraft:brown_concrete_powder",
                "minecraft:brown_glazed_terracotta",
                "minecraft:brown_mushroom_block",
                "minecraft:brown_terracotta",
                "minecraft:carved_pumpkin",
                "minecraft:chiseled_nether_bricks",
                "minecraft:chiseled_polished_blackstone",
                "minecraft:chiseled_quartz_block",
                "minecraft:chiseled_red_sandstone",
                "minecraft:chiseled_sandstone",
                "minecraft:chorus_flower",
                "minecraft:chorus_plant",
                "minecraft:clay",
                "minecraft:coal_block",
                "minecraft:coal_ore",
                "minecraft:coarse_dirt",
                "minecraft:cobblestone",
                "minecraft:cobweb",
                "minecraft:cracked_nether_bricks",
                "minecraft:cracked_polished_blackstone_bricks",
                "minecraft:crimson_nylium",
                "minecraft:cut_red_sandstone",
                "minecraft:cut_sandstone",
                "minecraft:cyan_concrete",
                "minecraft:cyan_concrete_powder",
                "minecraft:cyan_glazed_terracotta",
                "minecraft:cyan_terracotta",
                "minecraft:daylight_detector",
                "minecraft:dead_brain_coral_block",
                "minecraft:dead_bubble_coral_block",
                "minecraft:dead_fire_coral_block",
                "minecraft:dead_horn_coral_block",
                "minecraft:dead_tube_coral_block",
                "minecraft:diamond_block",
                "minecraft:diamond_ore",
                "minecraft:diorite",
                "minecraft:dirt",
                "minecraft:dried_kelp_block",
                "minecraft:emerald_block",
                "minecraft:emerald_ore",
                "minecraft:end_portal_frame",
                "minecraft:end_stone",
                "minecraft:end_stone_bricks",
                "minecraft:farmland",
                "minecraft:gilded_blackstone",
                "minecraft:gold_block",
                "minecraft:gold_ore",
                "minecraft:granite",
                "minecraft:grass_block",
                "minecraft:grass_path",
                "minecraft:gravel",
                "minecraft:gray_concrete",
                "minecraft:gray_concrete_powder",
                "minecraft:gray_glazed_terracotta",
                "minecraft:gray_terracotta",
                "minecraft:green_concrete",
                "minecraft:green_concrete_powder",
                "minecraft:green_glazed_terracotta",
                "minecraft:green_terracotta",
                "minecraft:hay_block",
                "minecraft:honey_block",
                "minecraft:honeycomb_block",
                "minecraft:infested_chiseled_stone_bricks",
                "minecraft:infested_cobblestone",
                "minecraft:infested_cracked_stone_bricks",
                "minecraft:infested_mossy_stone_bricks",
                "minecraft:infested_stone",
                "minecraft:infested_stone_bricks",
                "minecraft:iron_block",
                "minecraft:iron_ore",
                "minecraft:jukebox",
                "minecraft:lapis_block",
                "minecraft:lapis_ore",
                "minecraft:lectern",
                "minecraft:light_blue_concrete",
                "minecraft:light_blue_concrete_powder",
                "minecraft:light_blue_glazed_terracotta",
                "minecraft:light_blue_terracotta",
                "minecraft:light_gray_concrete",
                "minecraft:light_gray_concrete_powder",
                "minecraft:light_gray_glazed_terracotta",
                "minecraft:light_gray_terracotta",
                "minecraft:lime_concrete",
                "minecraft:lime_concrete_powder",
                "minecraft:lime_glazed_terracotta",
                "minecraft:lime_terracotta",
                "minecraft:lodestone",
                "minecraft:magenta_concrete",
                "minecraft:magenta_concrete_powder",
                "minecraft:magenta_glazed_terracotta",
                "minecraft:magenta_terracotta",
                "minecraft:melon",
                "minecraft:mossy_cobblestone",
                "minecraft:mushroom_stem",
                "minecraft:mycelium",
                "minecraft:nether_bricks",
                "minecraft:nether_gold_ore",
                "minecraft:nether_quartz_ore",
                "minecraft:nether_wart_block",
                "minecraft:netherite_block",
                "minecraft:netherrack",
                "minecraft:observer",
                "minecraft:obsidian",
                "minecraft:orange_concrete",
                "minecraft:orange_concrete_powder",
                "minecraft:orange_glazed_terracotta",
                "minecraft:orange_terracotta",
                "minecraft:packed_ice",
                "minecraft:pink_concrete",
                "minecraft:pink_concrete_powder",
                "minecraft:pink_glazed_terracotta",
                "minecraft:pink_terracotta",
                "minecraft:podzol",
                "minecraft:polished_andesite",
                "minecraft:polished_basalt",
                "minecraft:polished_blackstone",
                "minecraft:polished_blackstone_bricks",
                "minecraft:polished_diorite",
                "minecraft:polished_granite",
                "minecraft:prismarine",
                "minecraft:prismarine_bricks",
                "minecraft:pumpkin",
                "minecraft:purple_concrete",
                "minecraft:purple_concrete_powder",
                "minecraft:purple_glazed_terracotta",
                "minecraft:purple_terracotta",
                "minecraft:purpur_block",
                "minecraft:purpur_pillar",
                "minecraft:quartz_block",
                "minecraft:quartz_bricks",
                "minecraft:quartz_pillar",
                "minecraft:red_concrete",
                "minecraft:red_concrete_powder",
                "minecraft:red_glazed_terracotta",
                "minecraft:red_mushroom_block",
                "minecraft:red_nether_bricks",
                "minecraft:red_sandstone",
                "minecraft:red_terracotta",
                "minecraft:redstone_block",
                "minecraft:sandstone",
                "minecraft:slime_block",
                "minecraft:smithing_table",
                "minecraft:smooth_quartz",
                "minecraft:smooth_red_sandstone",
                "minecraft:smooth_sandstone",
                "minecraft:smooth_stone",
                "minecraft:snow_block",
                "minecraft:soul_sand",
                "minecraft:sponge",
                "minecraft:stone",
                "minecraft:target",
                "minecraft:terracotta",
                "minecraft:tnt",
                "minecraft:warped_nylium",
                "minecraft:warped_wart_block",
                "minecraft:wet_sponge",
                "minecraft:white_concrete",
                "minecraft:white_concrete_powder",
                "minecraft:white_glazed_terracotta",
                "minecraft:white_terracotta",
                "minecraft:yellow_concrete",
                "minecraft:yellow_concrete_powder",
                "minecraft:yellow_glazed_terracotta",
                "minecraft:yellow_terracotta"
        });

        EXPERIMENTAL_BLOCKS = new HardcodedBlockList(new String[]{
                "minecraft:barrel",
                "minecraft:blast_furnace",
                "minecraft:brewing_stand",
                "minecraft:cartography_table",
                "minecraft:crafting_table",
                "minecraft:dispenser",
                "minecraft:dropper",
                "minecraft:enchanting_table",
                "minecraft:fletching_table",
                "minecraft:furnace",
                "minecraft:ice",
                "minecraft:loom",
                "minecraft:note_block",
                "minecraft:piston",
                "minecraft:smoker",
                "minecraft:sticky_piston",
                "minecraft:stonecutter"
        });
    }

}
