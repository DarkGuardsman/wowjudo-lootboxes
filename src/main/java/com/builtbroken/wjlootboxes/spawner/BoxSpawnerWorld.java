package com.builtbroken.wjlootboxes.spawner;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public long timeToDelayBetweenChunkScans = TimeUnit.SECONDS.toMillis(1); //1 second
    /** How long to wait before scanning a world again */
    public long timeToDelayBetweenWorldScan = TimeUnit.MINUTES.toMillis(10); //10 mins

    /** How many chunks to scan each run before sleeping */
    public int chunksToScanPerRun = 100;

    /** How many boxes to spawn per chunk */
    public int boxesPerChunk = 1;

    /** Number of tries per chunk to place a box */
    public int triesPerChunk = 3;

    /** Max up and down y to move to find a free spot */
    public int upDownHeighAdjust = 5;

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

    /**
     * Called to load setting data
     *
     * @param fileForWorld
     */
    public void loadData(File fileForWorld)
    {
        try
        {
            if (fileForWorld.exists())
            {
                readDataFromFile(fileForWorld);
            }
            else
            {
                writeDataToFile(fileForWorld);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //JSON data for file
    public static final String JSON_DELAY_BETWEEN_SCANS = "delay_between_scans";
    public static final String JSON_DELAY_BETWEEN_CHUNK_SCANS = "delay_between_chunk_scans";
    public static final String JSON_DELAY_TO_RESCAN_CHUNK = "delay_to_rescan_chunk";
    public static final String JSON_CHUNKS_TO_SCAN = "chunks_to_scan";
    public static final String JSON_BOXES_PER_CHUNK = "boxes_per_chunk";
    public static final String JSON_TRIES_PER_CHUNK = "tries_per_chunk";
    public static final String JSON_HEIGHT_ADJUST = "height_adjust";
    public static final String JSON_CHANCES = "chances";
    public static final String JSON_BLOCKS = "blocks";

    //Json data for chance object
    public static final String JSON_CHANCE_TIER = "tier";
    public static final String JSON_CHANCE_VALUE = "chance";

    //Json data for block object
    public static final String JSON_BLOCK_ID = "id";
    public static final String JSON_BLOCK_META = "meta";

    protected void loadData(JsonElement element)
    {
        if (element.isJsonObject())
        {
            final JsonObject jsonData = element.getAsJsonObject();
        }
    }

    protected void saveData(JsonObject object)
    {
        object.add(JSON_DELAY_BETWEEN_SCANS, new JsonPrimitive(timeToDelayBetweenWorldScan));
        object.add(JSON_DELAY_BETWEEN_CHUNK_SCANS, new JsonPrimitive(timeToDelayBetweenChunkScans));
        object.add(JSON_DELAY_TO_RESCAN_CHUNK, new JsonPrimitive(timeToWaitBeforeScanningAChunkAgain));
        object.add(JSON_CHUNKS_TO_SCAN, new JsonPrimitive(chunksToScanPerRun));
        object.add(JSON_BOXES_PER_CHUNK, new JsonPrimitive(boxesPerChunk));
        object.add(JSON_TRIES_PER_CHUNK, new JsonPrimitive(triesPerChunk));
        object.add(JSON_HEIGHT_ADJUST, new JsonPrimitive(upDownHeighAdjust));

        //Load chance array
        JsonArray chanceArray = new JsonArray();
        for (int i = 0; i < chancePerTier.length; i++)
        {
            final float f = chancePerTier[i];

            JsonObject chanceObject = new JsonObject();
            chanceObject.add(JSON_CHANCE_TIER, new JsonPrimitive(i));
            chanceObject.add(JSON_CHANCE_VALUE, new JsonPrimitive(f));
            chanceArray.add(chanceObject);
        }
        object.add(JSON_CHANCES, chanceArray);

        //Load block array
        JsonArray blockArray = new JsonArray();
        for (Map.Entry<Block, List<Integer>> entry : supportedBlocks.entrySet())
        {
            if (entry.getKey() != null)
            {
                if (entry.getValue() == null || entry.getValue().isEmpty())
                {
                    JsonObject blockObject = new JsonObject();
                    blockObject.add(JSON_BLOCK_ID, new JsonPrimitive(Block.blockRegistry.getNameForObject(entry.getKey())));
                    blockArray.add(blockObject);
                }
                else
                {
                    for (int meta : entry.getValue())
                    {
                        JsonObject blockObject = new JsonObject();
                        blockObject.add(JSON_BLOCK_ID, new JsonPrimitive(Block.blockRegistry.getNameForObject(entry.getKey())));
                        blockObject.add(JSON_BLOCK_META, new JsonPrimitive(meta));
                        blockArray.add(blockObject);
                    }
                }
            }
        }
        object.add(JSON_BLOCKS, blockArray);
    }

    /**
     * Called to read settings data
     *
     * @param fileForWorld
     */
    public void readDataFromFile(File fileForWorld) throws IOException
    {
        //Load data
        FileReader stream = new FileReader(fileForWorld);
        BufferedReader reader = new BufferedReader(stream);
        JsonReader jsonReader = new JsonReader(reader);
        JsonElement element = Streams.parse(jsonReader);
        stream.close();

        loadData(element);
    }

    /**
     * Called to write settings data
     *
     * @param fileForWorld
     */
    public void writeDataToFile(File fileForWorld) throws IOException
    {
        //Generate JSON for output
        JsonObject object = new JsonObject();
        saveData(object);

        //Ensure the folder exists
        if (!fileForWorld.getParentFile().exists())
        {
            fileForWorld.getParentFile().mkdirs();
        }

        //Write file to disk
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter fileWriter = new FileWriter(fileForWorld))
        {
            fileWriter.write(gson.toJson(object));
        }
    }
}
