package me.florian.varlight;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_13_R2.NBTReadLimiter;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.logging.Level;

public class VarLightPlugin extends JavaPlugin implements Listener {

    public static final int NBT_COMPOUND = 0x0A;

    private Map<UUID, List<CustomLitBlock>> customLitBlocks = new HashMap<>();
    private final List<CustomLitBlock> emptyList = Collections.unmodifiableList(new ArrayList<>());

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        for (World world : Bukkit.getWorlds()) {
            try {
                customLitBlocks.put(world.getUID(), loadWorld(world));
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Exception while loading custom Light blocks for world " + world.getName(), e);
            }
        }
    }

    @Override
    public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            saveWorld(world);
        }

        customLitBlocks.clear();
    }

    private List<CustomLitBlock> loadWorld(World world) throws Exception {
        File customBlocks = new File(world.getWorldFolder(), "varlight.nbt");

        if (! customBlocks.exists()) {
            return new ArrayList<>();
        }

        List<CustomLitBlock> customLitBlocks = new ArrayList<>();

        byte[] buffer = new byte[(int) customBlocks.length()];

        FileInputStream fileInputStream = new FileInputStream(customBlocks);
        fileInputStream.read(buffer);
        fileInputStream.close();


        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.load(ByteStreams.newDataInput(buffer), 0, NBTReadLimiter.a);

        NBTTagList nbtTagList = nbtTagCompound.getList("Blocks", NBT_COMPOUND);

        for (int i = 0; i < nbtTagList.size(); i++) {
            customLitBlocks.add(CustomLitBlock.load(this, nbtTagList.getCompound(i), world));
        }

        return customLitBlocks;
    }

    private void saveWorld(World world) {
        try {
            File customBlocks = new File(world.getWorldFolder(), "varlight.nbt");

            if (! customBlocks.exists()) {
                customBlocks.createNewFile();
            }

            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            NBTTagList blocks = new NBTTagList();

            int i = 0;

            for (CustomLitBlock customLitBlock : getCustomLitBlocks(world.getUID())) {
                if (customLitBlock.isValid()) {
                    blocks.set(i++, customLitBlock.ToNbt());
                }

                Bukkit.getScheduler().cancelTask(customLitBlock.taskId);
            }

            nbtTagCompound.set("Blocks", blocks);

            FileOutputStream fileOutputStream = new FileOutputStream(customBlocks);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            nbtTagCompound.write(ByteStreams.newDataOutput(byteArrayOutputStream));
            byteArrayOutputStream.flush();
            byte[] buffer = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            fileOutputStream.write(buffer);
            fileOutputStream.flush();
            fileOutputStream.close();

            if (hasCustomLitBlocks(world)) {
                customLitBlocks.get(world.getUID()).clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected List<CustomLitBlock> getCustomLitBlocks(UUID worldUID) {
        return customLitBlocks.getOrDefault(worldUID, emptyList);
    }

    private boolean hasCustomLitBlocks(World world) {
        return customLitBlocks.containsKey(world.getUID()) && customLitBlocks.get(world.getUID()) != null && ! customLitBlocks.get(world.getUID()).isEmpty();
    }

    private void addCustomLitBlock(CustomLitBlock customLitBlock) {
        if (! hasCustomLitBlocks(customLitBlock.getWorld())) {
            List<CustomLitBlock> blockList = new ArrayList<>();

            blockList.add(customLitBlock);
            customLitBlocks.put(customLitBlock.getWorld().getUID(), blockList);
        } else {
            customLitBlocks.get(customLitBlock.getWorld().getUID()).add(customLitBlock);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent blockBreakEvent) {

    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent worldUnloadEvent) {
        saveWorld(worldUnloadEvent.getWorld());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (! e.isCancelled() && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) && CustomLitBlock.isValidBlockType(e.getClickedBlock().getType())) {
            if (e.getItem() != null && e.getItem().getType() == Material.GLOWSTONE_DUST) {

                int update = - 1;

                if (hasCustomLitBlocks(e.getPlayer().getWorld())) {
                    List<CustomLitBlock> customLitBlocks = getCustomLitBlocks(e.getPlayer().getWorld().getUID());

                    for (CustomLitBlock customLitBlock : customLitBlocks) {
                        if (customLitBlock.isBlock(e.getClickedBlock())) {
                            if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                                if ((update = customLitBlock.incrementLightLevel()) > 0 && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                                    e.getItem().setAmount(e.getItem().getAmount() - 1);
                                }
                            }
                            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                                e.setCancelled(e.getPlayer().getGameMode() == GameMode.CREATIVE);
                                update = customLitBlock.decrementLightLevel();
                            }

                            if (update == - 1) {
                                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Could not update block light"));
                            } else {
                                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Updated Block light to " + update));
                            }

                            return;
                        }
                    }
                }

                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    CustomLitBlock customLitBlock = new CustomLitBlock(this, e.getClickedBlock());
                    addCustomLitBlock(customLitBlock);

                    if ((update = customLitBlock.incrementLightLevel()) > 0 && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                        e.getItem().setAmount(e.getItem().getAmount() - 1);
                    }
                }

                if (update == - 1) {
                    e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Could not update block light"));
                } else {
                    e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Updated Block light to " + update));
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (! customLitBlocks.containsKey(e.getWorld().getUID())) {
            return;
        }

        for (CustomLitBlock customLitBlock : customLitBlocks.get(e.getWorld().getUID())) {
            if (customLitBlock.isAssociated(e)) {
                customLitBlock.onChunkLoad();
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        if (! customLitBlocks.containsKey(e.getWorld().getUID())) {
            return;
        }

        for (CustomLitBlock customLitBlock : customLitBlocks.get(e.getWorld().getUID())) {
            if (customLitBlock.isAssociated(e)) {
                customLitBlock.onChunkUnload();
            }
        }
    }
}
