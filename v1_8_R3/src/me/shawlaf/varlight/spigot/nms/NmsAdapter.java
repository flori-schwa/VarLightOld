package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.persistence.LightPersistFailedException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.util.IntPosition;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.Redstone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.*;

@ForMinecraft(version = "1.8.8")
public class NmsAdapter implements INmsAdapter {

    private static final BlockFace[] CHECK_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private static final byte GAME_INFO = 2;
    private final Class[] blacklistedDatas = new Class[]{
            Redstone.class,
            DirectionalContainer.class,
            PistonBaseMaterial.class
    };
    private final ItemStack varlightDebugStick;

    private Field longHashMapPlayerChunk;
    private Method broadcastPacket;

    @SuppressWarnings("unchecked")
    public NmsAdapter(VarLightPlugin plugin) {
        try {
            Class playerChunkClass = Class.forName("net.minecraft.server.v1_8_R3.PlayerChunkMap$PlayerChunk");

            longHashMapPlayerChunk = PlayerChunkMap.class.getDeclaredField("d");
            longHashMapPlayerChunk.setAccessible(true);

            broadcastPacket = playerChunkClass.getDeclaredMethod("a", Packet.class);
            broadcastPacket.setAccessible(true);
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = new net.minecraft.server.v1_8_R3.ItemStack(Items.STICK);

        nmsStack.addEnchantment(Enchantment.DURABILITY, 1);
        nmsStack.a("CustomType", new NBTTagString("varlight:debug_stick"));
        nmsStack.c = 1;

        this.varlightDebugStick = CraftItemStack.asBukkitCopy(nmsStack);

        ItemMeta meta = varlightDebugStick.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "VarLight Debug Stick");
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        varlightDebugStick.setItemMeta(meta);
    }


    private Object getPlayerChunk(Chunk chunk) throws IllegalAccessException {
        long encoded = (long) chunk.getX() + 2147483647L | (long) chunk.getZ() + 2147483647L << 32;
        return ((LongHashMap) longHashMapPlayerChunk.get(getNmsWorld(chunk.getWorld()).getPlayerChunkMap())).getEntry(encoded);
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
                Item nmsItem = Item.REGISTRY.get(key);
                return nmsItem == null ? null : CraftMagicNumbers.getMaterial(nmsItem);
            }

            case BLOCK: {
                net.minecraft.server.v1_8_R3.Block nmsBlock = net.minecraft.server.v1_8_R3.Block.REGISTRY.get(key);

                return nmsBlock == null ? null : CraftMagicNumbers.getMaterial(nmsBlock);
            }
        }

        return null;
    }

    @Override
    public boolean isCorrectTool(Material block, Material tool) {
        net.minecraft.server.v1_8_R3.Block nmsBlock = CraftMagicNumbers.getBlock(block);
        Item item = CraftMagicNumbers.getItem(tool);

        net.minecraft.server.v1_8_R3.ItemStack stack = new net.minecraft.server.v1_8_R3.ItemStack(item);

        return item.getDestroySpeed(stack, nmsBlock) > 1.0f;
    }

    @Override
    public String materialToKey(Material material) {
        return material.isBlock() ?
                net.minecraft.server.v1_8_R3.Block.REGISTRY.c(CraftMagicNumbers.getBlock(material)).toString() :
                Item.REGISTRY.c(CraftMagicNumbers.getItem(material)).toString();
    }

    @Override
    public String getLocalizedBlockName(Material material) {
        return CraftMagicNumbers.getBlock(material).getName();
    }

    @Override
    public Collection<String> getTypes(MaterialType type) {
        List<String> types = new ArrayList<>();

        switch (type) {
            case ITEM: {
                for (MinecraftKey key : Item.REGISTRY.keySet()) {
                    types.add(key.toString());
                    types.add(key.a());
                }

                return types;
            }

            case BLOCK: {
                for (MinecraftKey key : net.minecraft.server.v1_8_R3.Block.REGISTRY.keySet()) {
                    types.add(key.toString());
                    types.add(key.a());
                }

                return types;
            }
        }

        return types;
    }

    @Override
    public boolean isIllegalLightUpdateItem(Material material) {
        return material.isBlock();
    }

    @Override
    public boolean isBlockTransparent(@NotNull Block block) {
        return !getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock().getMaterial().blocksLight();
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

            IntPosition intPosition = toIntPosition(block.getLocation());

            for (BlockFace blockFace : CHECK_FACES) {
                IntPosition relative = intPosition.getRelative(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());

                if (relative.outOfBounds()) {
                    continue;
                }

                if (isBlockTransparent(toBlock(relative, world))) {
                    recalculateBlockLight(toLocation(relative, world));
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
        return getNmsWorld(block.getWorld()).getType(toBlockPosition(block.getLocation())).getBlock().r(); // TODO VERIFY
    }

    @Override
    public void sendChunkUpdates(@NotNull Chunk chunk, int mask) {
        try {
            WorldServer nmsWorld = getNmsWorld(chunk.getWorld());
            broadcastPacket.invoke(getPlayerChunk(chunk), new PacketPlayOutMapChunk(nmsWorld.getChunkAt(chunk.getX(), chunk.getZ()), false, mask));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isIllegalBlock(@NotNull Block block) {
        if (!block.getType().isBlock()) {
            return true;
        }

        if (getEmittingLightLevel(block) > 0) {
            return true;
        }

        Class<? extends MaterialData> data = block.getType().getData();

        for (Class blacklisted : blacklistedDatas) {
            if (blacklisted.isAssignableFrom(data)) {
                return true;
            }
        }

        if (block.getType() == Material.SLIME_BLOCK) {
            return true;
        }

        return !block.getType().isSolid() || !block.getType().isOccluding();
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        TextComponent textComponent = new TextComponent(message);
        PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(null, GAME_INFO);
        packetPlayOutChat.components = new BaseComponent[]{textComponent};

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutChat);
    }

    @Override
    public org.bukkit.inventory.ItemStack getVarLightDebugStick() {
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(varlightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.a("idLeast", new NBTTagLong(id.getLeastSignificantBits()));
        nmsStack.a("idMost", new NBTTagLong(id.getMostSignificantBits()));

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack makeGlowingStack(org.bukkit.inventory.ItemStack base, int lightLevel) {
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = new net.minecraft.server.v1_8_R3.ItemStack(
                CraftMagicNumbers.getItem(base.getType()),
                base.getAmount()
        );

        lightLevel &= 0xF;

        nmsStack.a("varlight:glowing", new NBTTagInt(lightLevel));

        org.bukkit.inventory.ItemStack stack = CraftItemStack.asBukkitCopy(nmsStack);

        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Glowing " + getLocalizedBlockName(stack.getType()));
        meta.setLore(Collections.singletonList(ChatColor.RESET + "Emitting Light: " + lightLevel));

        stack.setItemMeta(meta);

        return stack;
    }

    @Override
    public int getGlowingValue(org.bukkit.inventory.ItemStack glowingStack) {
        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(glowingStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null || !tag.hasKey("varlight:glowing")) {
            return -1;
        }

        return tag.getInt("varlight:glowing");
    }

    @Override
    public boolean isVarLightDebugStick(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.STICK) {
            return false;
        }

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null) {
            return false;
        }

        return tag.getString("CustomType").equals("varlight:debug_stick");
    }

    @Override
    public @NotNull File getRegionRoot(World world) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();

        ChunkProviderServer chunkProvider = nmsWorld.chunkProviderServer;

        try {
            IChunkLoader chunkLoader = ReflectionHelper.Safe.get(chunkProvider, "chunkLoader");

            ChunkRegionLoader regionLoader = ((ChunkRegionLoader) chunkLoader);

            return ReflectionHelper.Safe.get(regionLoader, "d");
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new LightPersistFailedException("Failed to determine world folder", e);
        }
    }

    @NotNull
    @Override
    public String getNumericMinecraftVersion() {
        return MinecraftServer.getServer().getVersion();
    }

    @Override
    public Block getTargetBlockExact(Player player, int maxDistance) {
        return player.getTargetBlock(new HashSet<Material>(), maxDistance);
    }
}
