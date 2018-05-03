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
    //---------------------------------------------------------------------------
    //--------Constants
    //---------------------------------------------------------------------------
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
    public static final String JSON_AREAS = "areas";

    //Json data for chance object
    public static final String JSON_CHANCE_TIER = "tier";
    public static final String JSON_CHANCE_VALUE = "chance";

    //Json data for block object
    public static final String JSON_BLOCK_ID = "id";
    public static final String JSON_BLOCK_META = "meta";

    //Json data for spawn areas
    public static final String JSON_START_X = "chunk_start_x";
    public static final String JSON_START_Z = "chunk_start_z";
    public static final String JSON_END_X = "chunk_end_x";
    public static final String JSON_END_Z = "chunk_end_z";

    //---------------------------------------------------------------------------
    //----- Settings
    //---------------------------------------------------------------------------
    /** Dimension of the world to access */
    public final int dimension;

    /** Block to meta data, used to check if blocks are supported for placing crates on */
    public final HashMap<Block, List<Integer>> supportedBlocks = new HashMap();

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
    public int placementCheckHeightAdjust = 5;

    public float[] chancePerTier = new float[]{0.3f, 0.2f, 0.1f, 0.05f, 0.01f};

    public List<BoxSpawnArea> allowedSpawnAreas = new ArrayList();

    //---------------------------------------------------------------------------

    /** Thread safe queue of blocks to place */
    public final ConcurrentLinkedQueue<BoxSpawnerPlacement> placementQueue = new ConcurrentLinkedQueue();


    public BoxSpawnerWorld(int dim)
    {
        this.dimension = dim;
        supportedBlocks.put(Blocks.grass, new ArrayList());
        supportedBlocks.put(Blocks.dirt, new ArrayList());
        allowedSpawnAreas.add(new BoxSpawnArea(-100, -100, 100, 100));
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
        if (world != null && canSpawnInArea(x, z))
        {
            //Check block
            Block block = world.getBlock(x, y, z);
            if (block != null && (block.isAir(world, x, y, z) || block.isReplaceable(world, x, y, z)))
            {
                return isSupportedBlock(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z));
            }
        }
        return false;
    }

    protected boolean canSpawnInArea(int x, int z)
    {
        //Check chunk
        if (!allowedSpawnAreas.isEmpty())
        {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            for (BoxSpawnArea area : allowedSpawnAreas)
            {
                if (area.isInside(chunkX, chunkZ))
                {
                    return true;
                }
            }
            return false;
        }
        return true;
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

    protected void loadData(JsonElement element)
    {
        if (element.isJsonObject())
        {
            final JsonObject jsonData = element.getAsJsonObject();
            timeToDelayBetweenWorldScan = jsonData.get(JSON_DELAY_BETWEEN_SCANS).getAsLong();
            timeToDelayBetweenChunkScans = jsonData.get(JSON_DELAY_BETWEEN_CHUNK_SCANS).getAsLong();
            timeToWaitBeforeScanningAChunkAgain = jsonData.get(JSON_DELAY_TO_RESCAN_CHUNK).getAsLong();

            chunksToScanPerRun = jsonData.get(JSON_CHUNKS_TO_SCAN).getAsInt();
            boxesPerChunk = jsonData.get(JSON_BOXES_PER_CHUNK).getAsInt();
            triesPerChunk = jsonData.get(JSON_TRIES_PER_CHUNK).getAsInt();
            placementCheckHeightAdjust = jsonData.get(JSON_HEIGHT_ADJUST).getAsInt();

            JsonArray chanceArray = jsonData.getAsJsonArray(JSON_CHANCES);
            for (JsonElement entry : chanceArray)
            {
                if (entry.isJsonObject())
                {
                    JsonObject chanceData = entry.getAsJsonObject();
                    int tier = chanceData.get(JSON_CHANCE_TIER).getAsInt();
                    float value = chanceData.get(JSON_CHANCE_VALUE).getAsFloat();

                    if (tier >= 0 && tier < WJLootBoxes.NUMBER_OF_TIERS && tier < this.chancePerTier.length)
                    {
                        this.chancePerTier[tier] = value;
                    }
                }
            }

            supportedBlocks.clear();
            JsonArray blockArray = jsonData.getAsJsonArray(JSON_BLOCKS);
            for (JsonElement entry : blockArray)
            {
                if (entry.isJsonObject())
                {
                    JsonObject blockObject = entry.getAsJsonObject();
                    String id = blockObject.getAsJsonPrimitive(JSON_BLOCK_ID).getAsString();
                    Block block = Block.getBlockFromName(id);
                    if (block != null)
                    {
                        if (!supportedBlocks.containsKey(block))
                        {
                            supportedBlocks.put(block, new ArrayList());
                        }
                        if (blockObject.has(JSON_BLOCK_META))
                        {
                            supportedBlocks.get(block).add(blockObject.get(JSON_BLOCK_META).getAsInt());
                        }
                    }
                    else
                    {
                        WJLootBoxes.LOGGER.error("BoxSpawnerWorld#loadData() - Failed to locate block '" + id + "' while load data for dimension '" + dimension + "'");
                    }
                }
            }

            if (jsonData.has(JSON_AREAS))
            {
                allowedSpawnAreas.clear();
                JsonArray areaArray = jsonData.getAsJsonArray(JSON_AREAS);
                for (JsonElement entry : areaArray)
                {
                    if (entry.isJsonObject())
                    {
                        JsonObject areaObject = entry.getAsJsonObject();
                        int startX = areaObject.get(JSON_START_X).getAsInt();
                        int startZ = areaObject.get(JSON_START_Z).getAsInt();
                        int endX = areaObject.get(JSON_END_X).getAsInt();
                        int endZ = areaObject.get(JSON_END_Z).getAsInt();
                        allowedSpawnAreas.add(new BoxSpawnArea(startX, startZ, endX, endZ));
                    }
                }
            }
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
        object.add(JSON_HEIGHT_ADJUST, new JsonPrimitive(placementCheckHeightAdjust));

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

        //Load block array
        JsonArray areaArray = new JsonArray();
        for (BoxSpawnArea area : allowedSpawnAreas)
        {
            JsonObject areaObject = new JsonObject();
            areaObject.add(JSON_START_X, new JsonPrimitive(area.startX));
            areaObject.add(JSON_START_Z, new JsonPrimitive(area.startZ));
            areaObject.add(JSON_END_X, new JsonPrimitive(area.endX));
            areaObject.add(JSON_END_Z, new JsonPrimitive(area.endZ));
            areaArray.add(areaObject);
        }
        object.add(JSON_AREAS, areaArray);
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
