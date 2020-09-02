package me.shawlaf.varlight.spigot.nms.v1_15_R1;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.nms.ForMinecraft;
import me.shawlaf.varlight.spigot.nms.INmsAdapter;
import me.shawlaf.varlight.spigot.nms.LightUpdateFailedException;
import me.shawlaf.varlight.spigot.nms.MaterialType;
import me.shawlaf.varlight.spigot.nms.v1_15_R1.wrappers.WrappedILightAccess;
import me.shawlaf.varlight.spigot.persistence.WorldLightSourceManager;
import me.shawlaf.varlight.spigot.updater.ILightUpdater;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joor.Reflect;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toLocation;

@ForMinecraft(version = "1.15.x")
public class NmsAdapter implements INmsAdapter {

    private final VarLightPlugin plugin;
    private final ItemStack varlightDebugStick;

    public NmsAdapter(VarLightPlugin plugin) {
        this.plugin = plugin;

        net.minecraft.server.v1_15_R1.ItemStack nmsStack = new net.minecraft.server.v1_15_R1.ItemStack(Items.STICK);

        nmsStack.addEnchantment(Enchantments.DURABILITY, 1);
        nmsStack.a("CustomType", NBTTagString.a("varlight:debug_stick"));

        this.varlightDebugStick = CraftItemStack.asBukkitCopy(nmsStack);
        ItemMeta meta = varlightDebugStick.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "VarLight Debug Stick");
        varlightDebugStick.setItemMeta(meta);
    }

    @Override
    public void enableVarLightInWorld(@NotNull World world) {
        WorldServer nmsWorld = getNmsWorld(world);
        WrappedILightAccess wrappedILightAccess = new WrappedILightAccess(plugin, nmsWorld);

        LightEngineThreaded let = nmsWorld.getChunkProvider().getLightEngine();

        LightEngineBlock leb = ((LightEngineBlock) let.a(EnumSkyBlock.BLOCK));

        Reflect.on(leb).set("a", wrappedILightAccess);
    }

    @Override
    @Nullable
    public Material keyToType(String namespacedKey, MaterialType type) {
        MinecraftKey key = new MinecraftKey(namespacedKey);

        switch (type) {
            case ITEM: {
                return CraftMagicNumbers.getMaterial(IRegistry.ITEM.get(key));
            }

            case BLOCK: {
                return CraftMagicNumbers.getMaterial(IRegistry.BLOCK.get(key));
            }
        }

        return null;
    }

    @NotNull
    @Override
    public String getLocalizedBlockName(Material material) {
        return LocaleLanguage.a().a(CraftMagicNumbers.getBlock(material).k());
    }

    @NotNull
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
    public boolean isIllegalBlock(@NotNull Material material) {
        if (HardcodedBlockList.ALLOWED_BLOCKS.contains(material)) {
            return false;
        }

        if (plugin.getConfiguration().isAllowExperimentalBlocks()) {
            return !HardcodedBlockList.EXPERIMENTAL_BLOCKS.contains(material);
        }

        return true;
    }

    @NotNull
    @Override
    public ItemStack getVarLightDebugStick() {
        net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(varlightDebugStick);

        UUID id = UUID.randomUUID();

        nmsStack.a("idLeast", NBTTagLong.a(id.getLeastSignificantBits()));
        nmsStack.a("idMost", NBTTagLong.a(id.getMostSignificantBits()));

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public ItemStack makeGlowingStack(ItemStack base, int lightLevel) {
        net.minecraft.server.v1_15_R1.ItemStack nmsStack = new net.minecraft.server.v1_15_R1.ItemStack(
                CraftMagicNumbers.getItem(base.getType()),
                base.getAmount()
        );

        lightLevel &= 0xF;

        nmsStack.a("varlight:glowing", NBTTagInt.a(lightLevel));

        ItemStack stack = CraftItemStack.asBukkitCopy(nmsStack);

        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Glowing " + getLocalizedBlockName(stack.getType()));
        meta.setLore(Collections.singletonList(ChatColor.RESET + "Emitting Light: " + lightLevel));

        stack.setItemMeta(meta);

        return stack;
    }

    @Override
    public int getGlowingValue(ItemStack glowingStack) {
        net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(glowingStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null || !tag.hasKey("varlight:glowing")) {
            return -1;
        }

        return tag.getInt("varlight:glowing");
    }

    @Override
    public boolean isVarLightDebugStick(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.STICK) {
            return false;
        }

        net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);

        NBTTagCompound tag = nmsStack.getTag();

        if (tag == null) {
            return false;
        }

        return tag.getString("CustomType").equals("varlight:debug_stick");
    }

    @Override
    public @NotNull File getRegionRoot(World world) {
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        return Reflect.on(nmsWorld.getChunkProvider().playerChunkMap).get("w");
    }

    private WorldServer getNmsWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }

    // region Light IO

    @Override
    public CompletableFuture<Void> updateChunk(World world, ChunkCoords chunkCoords) {
        WorldServer worldServer = getNmsWorld(world);

        LightEngine let = worldServer.e();

        WorldLightSourceManager manager = plugin.getManager(worldServer.getWorld());

        if (manager == null) {
            return CompletableFuture.completedFuture(null);
        }

        IChunkAccess iChunkAccess = worldServer.getChunkProvider().a(chunkCoords.x, chunkCoords.z);

        if (iChunkAccess == null) {
            throw new LightUpdateFailedException("Could not fetch IChunkAccess at coords " + chunkCoords.toShortString());
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        ((LightEngineThreaded) let).a(iChunkAccess, true).thenRun(() -> future.complete(null));

        return future;
    }

    @Override
    public void sendLightUpdates(World world, ChunkCoords center) {
        WorldServer nmsWorld = getNmsWorld(world);
        LightEngine let = nmsWorld.e();

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in world " + world.getName());
        }

        for (ChunkCoords toSendClientUpdate : collectChunkPositionsToUpdate(center)) {
            ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(toSendClientUpdate.x, toSendClientUpdate.z);
            PacketPlayOutLightUpdate ppolu = new PacketPlayOutLightUpdate(chunkCoordIntPair, let);

            nmsWorld.getChunkProvider().playerChunkMap.a(chunkCoordIntPair, false).forEach(e -> e.playerConnection.sendPacket(ppolu));
        }
    }

    @Override
    public void setLight(Location location, int lightLevel) {
        WorldLightSourceManager manager = plugin.getManager(location.getWorld());

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in World " + location.getWorld().getName());
        }

        manager.setCustomLuminance(location, lightLevel);
    }

    @Override
    public void setLight(World world, IntPosition position, int lightLevel) {
        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in World " + world.getName());
        }

        manager.setCustomLuminance(position, lightLevel);
    }

    @Override
    public CompletableFuture<Void> setAndUpdateLight(World world, IntPosition position, int lightLevel) {
        return setAndUpdateLight0(toLocation(position, world), position, lightLevel);
    }

    @Override
    public CompletableFuture<Void> setAndUpdateLight(Location location, int lightLevel) {
        return setAndUpdateLight0(location, toIntPosition(location), lightLevel);
    }

    @Override
    public CompletableFuture<Void> updateLight(Location location) {
        return updateLight0(location, toIntPosition(location));
    }

    @Override
    public CompletableFuture<Void> updateLight(World world, Collection<IntPosition> positions) {
        WorldServer nmsWorld = getNmsWorld(world);

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in world " + world.getName());
        }

        LightEngineBlock leb = ((LightEngineBlock) nmsWorld.e().a(EnumSkyBlock.BLOCK));

        return scheduleToLightMailbox(nmsWorld, () -> {
            for (IntPosition position : positions) {
                leb.a(new BlockPosition(position.x, position.y, position.z));
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateLight(World world, IntPosition position) {
        return updateLight0(toLocation(position, world), position);
    }

    @Override
    public CompletableFuture<Void> recalculateChunk(Chunk chunk) {
        return recalculateChunk(chunk.getWorld(), new ChunkCoords(chunk.getX(), chunk.getZ()));
    }

    @Override
    public CompletableFuture<Void> recalculateChunk(World world, ChunkCoords chunkCoords) {
        WorldServer nmsWorld = getNmsWorld(world);

        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in world " + world.getName());
        }

        LightEngineBlock leb = ((LightEngineBlock) nmsWorld.e().a(EnumSkyBlock.BLOCK));

        return scheduleToLightMailbox(nmsWorld, () -> {
            StreamSupport.stream(BlockPosition.b(
                    chunkCoords.getCornerAX(),
                    chunkCoords.getCornerAY(),
                    chunkCoords.getCornerAZ(),
                    chunkCoords.getCornerBX(),
                    chunkCoords.getCornerBY(),
                    chunkCoords.getCornerBZ()
            ).spliterator(), false).forEach(leb::a);
        });
    }

    private CompletableFuture<Void> setAndUpdateLight0(Location location, IntPosition position, int lightLevel) {
        setLight(location, lightLevel);

        return updateLight0(location, position);
    }

    private CompletableFuture<Void> updateLight0(Location location, IntPosition position) {

        WorldServer nmsWorld = getNmsWorld(location.getWorld());

        WorldLightSourceManager manager = plugin.getManager(location.getWorld());

        if (manager == null) {
            throw new LightUpdateFailedException("VarLight not enabled in world " + location.getWorld().getName());
        }

        LightEngineBlock leb = ((LightEngineBlock) nmsWorld.e().a(EnumSkyBlock.BLOCK));

        ChunkCoords center = position.toChunkCoords();

        return scheduleToLightMailbox(nmsWorld,
                () -> leb.a(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ())))
                .thenRunAsync(
                        () -> {
                            Collection<ChunkCoords> neighbours = plugin.getNmsAdapter().collectChunkPositionsToUpdate(position);
                            List<CompletableFuture<Void>> futures = new ArrayList<>(neighbours.size());

                            for (ChunkCoords neighbour : neighbours) {
                                futures.add(plugin.getNmsAdapter().updateChunk(location.getWorld(), neighbour));
                            }

                            plugin.getBukkitAsyncExecutorService().submit(() -> {
                                futures.forEach(CompletableFuture::join);
                            }).thenRunAsync(() -> plugin.getNmsAdapter().sendLightUpdates(location.getWorld(), center), plugin.getBukkitMainThreadExecutorService());
                        }, plugin.getBukkitMainThreadExecutorService()
                );
    }

    private CompletableFuture<Void> scheduleToLightMailbox(WorldServer worldServer, Runnable task) {
        return scheduleToLightMailbox(((LightEngineThreaded) worldServer.e()), task);
    }

    private CompletableFuture<Void> scheduleToLightMailbox(LightEngineThreaded lightEngine, Runnable task) {
        ThreadedMailbox<Runnable> mailbox = Reflect.on(lightEngine).get("b");
        CompletableFuture<Void> future = new CompletableFuture<>();

        mailbox.a(() -> {
            task.run();

            future.complete(null);
        });

        return future;
    }

    // endregion
}

