package me.shawlaf.varlight.nms;


import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.util.IntPosition;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Openable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

@ForMinecraft(version = "1.13.2")
public class NmsAdapter implements INmsAdapter {

    private static final BlockFace[] CHECK_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };
    private final org.bukkit.inventory.ItemStack varlightDebugStick;

    private Field lightBlockingField;

    public NmsAdapter(VarLightPlugin plugin) {
        try {
            lightBlockingField = net.minecraft.server.v1_13_R2.Block.class.getDeclaredField("n");
            lightBlockingField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        ItemStack nmsStack = new ItemStack(Items.STICK);

        nmsStack.addEnchantment(Enchantments.DURABILITY, 1);
        nmsStack.a("CustomType", new NBTTagString("varlight:debug_stick"));

        this.varlightDebugStick = CraftItemStack.asBukkitCopy(nmsStack);
        ItemMeta meta = varlightDebugStick.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "VarLight Debug Stick");
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        varlightDebugStick.setItemMeta(meta);
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    private BlockPosition toBlockPosition(Location location) {
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    @Nullable
    public Material keyToType(String namespacedKey, MaterialType type) {
        MinecraftKey key = new MinecraftKey(namespacedKey);

        switch (type) {
            case ITEM: {
                Item nmsItem = IRegistry.ITEM.get(key);
                return nmsItem == null ? null : CraftMagicNumbers.getMaterial(nmsItem);
            }

            case BLOCK: {
                net.minecraft.server.v1_13_R2.Block nmsBlock = IRegistry.BLOCK.get(key);

                return nmsBlock == null ? null : CraftMagicNumbers.getMaterial(nmsBlock);
            }
        }

        return null;
    }

    @Override
    public String materialToKey(Material material) {
        return material.isBlock() ?
                Optional.ofNullable(IRegistry.BLOCK.getKey(CraftMagicNumbers.getBlock(material))).map(Object::toString).orElse(null) :
                Optional.ofNullable(IRegistry.ITEM.getKey(CraftMagicNumbers.getItem(material))).map(Objects::toString).orElse(null);
    }

    @Override
    public Collection<String> getTypes(MaterialType type) {
        List<String> types = new ArrayList<>();

        switch (type) {
            case ITEM: {
                for (MinecraftKey key : IRegistry.ITEM.keySet()) {
                    types.add(key.toString());
                    types.add(key.getKey());
                }

                return types;
            }

            case BLOCK: {
                for (MinecraftKey key : IRegistry.BLOCK.keySet()) {
                    types.add(key.toString());
                    types.add(key.getKey());
                }

                return types;
            }
        }

        return types;
    }

    @Override
    public boolean isIllegalLightUpdateItem(Material material) {
        return material.isBlock() || !material.isItem();
    }

    @Override
    public boolean isBlockTransparent(@NotNull Block block) {
        try {
            return !lightBlockingField.getBoolean(getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void recalculateBlockLight(Location at) {
        getNmsWorld(at.getWorld()).c(EnumSkyBlock.BLOCK, toBlockPosition(at));
    }

    @Override
    public void updateBlockLight(@NotNull Location at, int lightLevel) {
        Block block = at.getBlock();
        World world = at.getWorld();

        recalculateBlockLight(at);

        if (lightLevel > 0) {
            getNmsWorld(world).a(EnumSkyBlock.BLOCK, toBlockPosition(at), lightLevel);

            IntPosition intPosition = new IntPosition(block.getLocation());

            for (BlockFace blockFace : CHECK_FACES) {
                IntPosition relative = intPosition.getRelative(blockFace);

                if (relative.outOfBounds()) {
                    continue;
                }

                if (isBlockTransparent(relative.toBlock(world))) {
                    recalculateBlockLight(relative.toLocation(world));
                }
            }
        }

        List<Chunk> chunksToUpdate = collectChunksToUpdate(at);

        int mask = getChunkBitMask(at);

        for (Chunk chunk : chunksToUpdate) {
            sendChunkUpdates(chunk, mask);
        }
    }

    @Override
    public int getEmittingLightLevel(@NotNull Block block) {
        return ((CraftWorld) block.getWorld()).getHandle().getChunkAt(block.getChunk().getX(), block.getChunk().getZ()).getBlockData(block.getX(), block.getY(), block.getZ()).e();
    }

    @Override
    public void sendChunkUpdates(@NotNull Chunk chunk, int mask) {
        WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
        PlayerChunkMap playerChunkMap = nmsWorld.getPlayerChunkMap();
        PlayerChunk playerChunk = playerChunkMap.getChunk(chunk.getX(), chunk.getZ());

        Objects.requireNonNull(playerChunk);

        for (int cy = 0; cy < 16; cy++) {
            if ((mask & (1 << cy)) == 0) {
                continue;
            }

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        playerChunk.a(x, cy * 16 + y, z);
                    }
                }
            }
        }

        playerChunk.d();
    }

    @Override
    public boolean isIllegalBlock(@NotNull Block block) {
        if (!block.getType().isBlock()) {
            return true;
        }

        if (getEmittingLightLevel(block) > 0) {
            return true;
        }

        BlockData blockData = block.getType().createBlockData();

        if (blockData instanceof Powerable || blockData instanceof AnaloguePowerable || blockData instanceof Openable || blockData instanceof Piston) {
            return true;
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return true;
        }

        if (block.getType() == Material.BLUE_ICE) {
            return false; // Packed ice is solid and occluding but blue ice isn't?
        }

        return !block.getType().isSolid() || !block.getType().isOccluding();
    }

    @SuppressWarnings("deprecation")
    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    @Override
    public org.bukkit.inventory.ItemStack getVarLightDebugStick() {
        ItemStack nmsStack = CraftItemStack.asNMSCopy(varlightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.a("idLeast", new NBTTagLong(id.getLeastSignificantBits()));
        nmsStack.a("idMost", new NBTTagLong(id.getMostSignificantBits()));

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public boolean isVarLightDebugStick(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.STICK) {
            return false;
        }

        net.minecraft.server.v1_13_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null) {
            return false;
        }

        return tag.getString("CustomType").equals("varlight:debug_stick");
    }

    @Override
    public Block getTargetBlockExact(Player player, int maxDistance) {
        return player.getTargetBlockExact(maxDistance, FluidCollisionMode.NEVER);
    }
}
