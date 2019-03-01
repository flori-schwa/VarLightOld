package me.florian.varlight;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.world.ChunkEvent;
import org.bukkit.plugin.Plugin;
import ru.beykerykt.lightapi.LightAPI;

public class CustomLitBlock {

    private World world;
    private IntPosition position;
    private Block wrappedBlock;
    private int customLightLevel;
    protected int taskId;

    public static boolean isValidBlockType(Material material) {
        return material.isSolid();
    }

    protected static CustomLitBlock load(Plugin plugin, NBTTagCompound nbtTagCompound, World world) {
        CustomLitBlock customLitBlock = new CustomLitBlock();

        customLitBlock.world = world;
        customLitBlock.position = new IntPosition(nbtTagCompound.getLong("Location"));
        customLitBlock.customLightLevel = nbtTagCompound.getByte("Light");

        if (customLitBlock.isChunkLoaded()) {
            customLitBlock.wrappedBlock = world.getBlockAt(customLitBlock.position.getX(), customLitBlock.position.getY(), customLitBlock.position.getZ());
        }

        customLitBlock.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (customLitBlock.isChunkLoaded()) {
                if (!isValidBlockType(customLitBlock.wrappedBlock.getType())) {
                    customLitBlock.setCustomLightLevel(0);
                }
            }
        }, 10L, 10L);

        return customLitBlock;
    }

    private CustomLitBlock() {

    }

    public CustomLitBlock(Plugin plugin, Block block) {
        this.world = block.getWorld();
        this.position = new IntPosition(block.getX(), block.getY(), block.getZ());
        this.wrappedBlock = block;
        this.customLightLevel = 0;

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {

            if (isChunkLoaded()) {
                if (!isValidBlockType(wrappedBlock.getType())) {
                    setCustomLightLevel(0);
                }
            }

        }, 10L, 10L);
    }

    public boolean isBlock(Block block) {
        return position.getX() == block.getX() && position.getY() == block.getY() && position.getZ() == block.getZ();
    }


    public boolean isAssociated(BlockPosition blockPosition) {
        return position.getX() == blockPosition.getX() && position.getY() == blockPosition.getY() && position.getZ() == blockPosition.getZ();
    }

    public boolean isAssociated(ChunkEvent chunkEvent) {
        return chunkEvent.getChunk().getX() == position.getChunkX() && chunkEvent.getChunk().getZ() == position.getChunkZ() && chunkEvent.getWorld().getUID().equals(world.getUID());
    }

    protected void onChunkLoad() {
        LightAPI.createLight(world, position.getX(), position.getY(), position.getZ(), customLightLevel, false);
        update();
        this.wrappedBlock = world.getBlockAt(position.getX(), position.getY(), position.getZ());
    }

    protected void onChunkUnload() {
        LightAPI.deleteLight(world, position.getX(), position.getY(), position.getZ(), false);
        update();
        this.wrappedBlock = null;
    }

    public Block getWrappedBlock() {
        return wrappedBlock;
    }

    public NBTTagCompound ToNbt() {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();

        nbtTagCompound.setLong("Location", position.encode());
        nbtTagCompound.setByte("Light", (byte) customLightLevel);

        return nbtTagCompound;
    }

    public int getCustomLightLevel() {
        return customLightLevel;
    }

    public World getWorld() {
        return world;
    }

    public int incrementLightLevel() {
        if (customLightLevel == 15) {
            return - 1;
        }

        setCustomLightLevel(customLightLevel + 1);
        return customLightLevel;
    }

    public int decrementLightLevel() {
        if (customLightLevel == 0) {
            return - 1;
        }

        setCustomLightLevel(customLightLevel - 1);
        return customLightLevel;
    }

    public void setCustomLightLevel(int customLightLevel) {
        if (customLightLevel < 0 || customLightLevel > 15) {
            throw new IllegalArgumentException("Invalid Light Level");
        }

        this.customLightLevel = customLightLevel;

        if (isChunkLoaded()) {

            LightAPI.deleteLight(world, position.getX(), position.getY(), position.getZ(), false);

            if (customLightLevel > 0) {
                LightAPI.createLight(world, position.getX(), position.getY(), position.getZ(), customLightLevel, false);
            }

            update();
        }
    }

    private void update() {
        LightAPI.updateChunk(world, position.getX(), position.getY(), position.getZ(), world.getPlayers());
    }

    public boolean isChunkLoaded() {
        return world.isChunkLoaded(position.getChunkX(), position.getChunkZ());
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + position.hashCode();
        result = 31 * result + world.hashCode();

        return result;
    }

    public boolean isValid() {
        return isValidBlockType(wrappedBlock.getType());
    }
}
