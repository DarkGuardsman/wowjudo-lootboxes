package com.builtbroken.wjlootboxes.spawner;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import com.builtbroken.wjlootboxes.box.TileEntityLootbox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;

import java.util.*;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/4/2018.
 */
public class BoxSpawnerThread extends Thread
{
    /** Checked in all loops in order to kill the thread if false */
    public boolean shouldRun = true;

    /** Current world being scanned, using dim id to be thread safe */
    private int currentScanningWorld = 0;

    /** Map of worlds, to maps of chunks and when they were scanned last( in milli-seconds) */
    private HashMap<Integer, HashMap<ChunkCoordIntPair, Long>> lastScanTimes = new HashMap();

    /** Map of worlds to last time scanned */
    private HashMap<Integer, Long> lastWorldScanTimes = new HashMap();

    @Override
    public void run()
    {
        WJLootBoxes.LOGGER.info("Scanner thread has started!");
        while (shouldRun)
        {
            try
            {
                final World currentScanWorld = world();
                final BoxSpawnerWorld settings = WJLootBoxes.boxSpawner.get(currentScanWorld);
                if (currentScanWorld instanceof WorldServer && settings != null)
                {
                    final WorldServer world = (WorldServer) world();
                    //Get scan times for chunks
                    HashMap<ChunkCoordIntPair, Long> lastScanned = lastScanTimes.get(currentScanningWorld); //TODO maybe clear really old values?
                    if (lastScanned == null)
                    {
                        lastScanned = new HashMap();
                    }

                    //Get list of chunks to scan
                    Queue<Chunk> que = new LinkedList();
                    que.addAll(world.theChunkProviderServer.loadedChunks);

                    int chunksScanned = 0;
                    //Loop until we run out of stuff
                    while (!que.isEmpty() && shouldRun)
                    {
                        //Get next chunk
                        Chunk chunk = que.poll();
                        if (chunk != null)
                        {
                            //Ensure we have not scanned it yet
                            ChunkCoordIntPair pair = chunk.getChunkCoordIntPair();
                            if (!lastScanned.containsKey(pair) || (System.currentTimeMillis() - lastScanned.get(pair)) >= settings.timeToWaitBeforeScanningAChunkAgain)
                            {
                                //Mark as scanned
                                lastScanned.put(pair, System.currentTimeMillis());

                                //Handle
                                handleChunk(settings, world, chunk);

                                //Keep track of chunks scanned
                                chunksScanned++;
                            }

                            //Sleep if we have scanned enough chunks (helps free up CPU on busy servers)
                            if (chunksScanned > settings.chunksToScanPerRun && settings.timeToDelayBetweenChunkScans > 0)
                            {
                                sleep(settings.timeToDelayBetweenChunkScans);
                            }
                        }
                    }

                    //Keep track of the last time we scanned
                    lastScanTimes.put(currentScanningWorld, lastScanned);
                    lastWorldScanTimes.put(currentScanningWorld, System.currentTimeMillis());
                }

                //Move to next world
                nextWorld();

                //If current world is the same as last, delay until next scan time
                if (currentScanWorld == world() && settings != null && settings.timeToDelayBetweenWorldScan > 0)
                {
                    sleep(settings.timeToDelayBetweenWorldScan);
                }
            }
            catch (Exception e)
            {
                WJLootBoxes.LOGGER.error("Scanned thread has experience an unexpected error, but has recovered", e);
            }
        }
        WJLootBoxes.LOGGER.info("Scanner Thread has stopped");
    }

    private void handleChunk(BoxSpawnerWorld settings, World world, Chunk chunk)
    {
        int crates = countCrates(chunk);
        if (crates < settings.boxesPerChunk)
        {
            //Try so many times to spawn boxes
            for (int i = 0; i < settings.boxesPerChunk; i++)
            {
                //Get data
                int tier = world.rand.nextInt(WJLootBoxes.NUMBER_OF_TIERS);
                float chance = settings.chancePerTier[tier];

                //Randomize
                if (chance > world.rand.nextFloat())
                {
                    //try 3 times to find a usable block
                    out:
                    // exit point for loop
                    for (int c = 0; c < settings.triesPerChunk; c++)
                    {
                        //random position inside chunk
                        int x = 8 - world.rand.nextInt(8) + world.rand.nextInt(7);
                        int z = 8 - world.rand.nextInt(8) + world.rand.nextInt(7);
                        int y = world.rand.nextInt(chunk.getHeightValue(x, z));

                        //Allow a few up and down positions
                        for (int yz = y - settings.placementCheckHeightAdjust; yz < (settings.placementCheckHeightAdjust + y); yz++)
                        {
                            //Offset by
                            int xz = chunk.xPosition * 16;
                            int zz = chunk.zPosition * 16;

                            if (settings.canSpawnHere(xz, yz, zz))
                            {
                                System.out.println(String.format("Generated spawn point for box %d %d %d %d", settings.dimension, xz, yz, yz));
                                settings.placementQueue.add(new BoxSpawnerPlacement(xz, yz, zz, tier));
                                break out;
                            }
                        }
                    }
                }
            }
        }
    }

    private int countCrates(Chunk chunk)
    {
        int count = 0;
        Map<ChunkPosition, TileEntity> tileEntityMap = chunk.chunkTileEntityMap;
        ArrayList<TileEntity> c = new ArrayList<>(tileEntityMap.values().size());
        c.addAll(tileEntityMap.values());
        for (int i = 0; i < c.size(); i++)
        {
            TileEntity tile = c.get(i);
            if (tile instanceof TileEntityLootbox)
            {
                count += 1;
            }
        }
        return count;
    }

    private World world()
    {
        World world = DimensionManager.getWorld(currentScanningWorld);
        while (world == null)
        {
            nextWorld();
            world = DimensionManager.getWorld(currentScanningWorld);
        }
        return world;
    }

    private void nextWorld()
    {
        //TODO next world in list of worlds
    }

    public void startScanner()
    {
        shouldRun = true;
        if (!isAlive())
        {
            start();
        }
    }

    public void stopScanner()
    {
        shouldRun = false;
    }

    public void kill()
    {
        stopScanner();
        lastScanTimes.clear();
    }
}
