package com.builtbroken.wjlootboxes.spawner;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tracks settings and data about the world in order to spawn boxes
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/23/2018.
 */
public class BoxSpawnerWorld
{
    /** Dimension of the world to access */
    public final int dimension;

    /** Block to meta data, used to check if blocks are supported for placing crates on */
    public final HashMap<Block, List<Integer>> supportedBlocks = new HashMap();

    /** Thread safe queue of blocks to place */
    public final ConcurrentLinkedQueue<BoxSpawnerPlacement> placementQueue = new ConcurrentLinkedQueue();

    /** How long to wait before scanning a chunk again */
    public long timeToWaitBeforeScanningAChunkAgain = TimeUnit.MINUTES.toMillis(10); //10 mins
    /** How long to wait before scanning the next chunk */
    public long timeToDelayBetweenChunkScans = TimeUnit.MINUTES.toMillis(1); //1 mins
    /** How long to wait before scanning a world again */
    public long timeToDelayBetweenWorldScan = TimeUnit.MINUTES.toMillis(10); //10 mins
    /** How many boxes to spawn per chunk */
    public int boxesPerChunk = 1;

    public int triesPerChunk = 5;
    public float[] chancePerTier = new float[]{0.3f, 0.2f, 0.1f, 0.05f, 0.01f};


    public BoxSpawnerWorld(int dim)
    {
        this.dimension = dim;
        supportedBlocks.put(Blocks.grass, new ArrayList());
        supportedBlocks.put(Blocks.dirt, new ArrayList());
    }

    /**
     * Called each world tick
     *
     * @param world
     * @param phase
     */
    public void update(World world, TickEvent.Phase phase)
    {
        if (phase == TickEvent.Phase.END)
        {
            while (!placementQueue.isEmpty())
            {
                BoxSpawnerPlacement placement = placementQueue.poll();
                if (placement != null && canSpawnHere(placement.chunkPosX, placement.chunkPosY, placement.chunkPosZ))
                {
                    world.setBlock(placement.chunkPosX, placement.chunkPosY, placement.chunkPosZ,
                            WJLootBoxes.blockLootbox, placement.tier, 3);
                    System.out.println(String.format("Placed box %d %d %d %d", dimension, placement.chunkPosX, placement.chunkPosY, placement.chunkPosZ));
                }
            }
        }
    }

    /**
     * Called by thread to check if the crate can be placec
     *
     * @param x
     * @param y
     * @param z
     * @return true if can be placed
     */
    public boolean canSpawnHere(int x, int y, int z)
    {
        World world = world();
        if (world != null)
        {
            Block block = world.getBlock(x, y, z);
            if (block != null && (block.isAir(world, x, y, z) || block.isReplaceable(world, x, y, z)))
            {
                return isSupportedBlock(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z));
            }
        }
        return false;
    }

    public boolean isSupportedBlock(Block block)
    {
        return isSupportedBlock(block, -1);
    }

    public boolean isSupportedBlock(Block block, int meta)
    {
        return supportedBlocks.containsKey(block) && (meta == -1 || supportedBlocks.get(block).isEmpty() || supportedBlocks.get(block).contains(meta));
    }

    public World world()
    {
        return DimensionManager.getWorld(dimension);
    }
}
