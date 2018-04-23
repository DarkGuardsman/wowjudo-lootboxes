package com.builtbroken.wjlootboxes.loot;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles loading, saving, storing, and distributing loot randomly
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class LootHandler
{
    public static final String JSON_MIN_LOOT = "loot_min_count";
    public static final String JSON_MAX_LOOT = "loot_max_count";
    public static final String JSON_LOOT_ARRAY = "loot_entries";

    public static final String JSON_ITEM_ID = "item";
    public static final String JSON_ITEM_META = "data";
    public static final String JSON_ITEM_MIN_COUNT = "min_count";
    public static final String JSON_ITEM_MAX_COUNT = "max_count";
    public static final String JSON_ITEM_CHANCE = "chance";

    /** Tiers of loot boxes that exist */
    public final int tiers;
    /** Array of loot tables for each tier */
    public final LootEntry[][] loot;
    /** Number of items to spawn per tier */
    public final int[] minLootCount;
    /** Number of items to spawn per tier */
    public final int[] maxLootCount;
    /** Number of items to spawn per tier */
    public final boolean[] allowDuplicateDrops;

    private String lootDataPath = "./loot";
    private File lootDataFolder;

    public LootHandler(int numberOfTiers)
    {
        tiers = numberOfTiers;
        loot = new LootEntry[numberOfTiers][];
        minLootCount = new int[numberOfTiers];
        maxLootCount = new int[numberOfTiers];
        allowDuplicateDrops = new boolean[numberOfTiers];
    }

    /**
     * Called to drop random loot at the location
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param tier
     */
    public void dropRandomLoot(World world, int x, int y, int z, int tier)
    {
        //Get loot to spawn
        LootEntry[] possibleItems = loot[tier];

        //Get items to spawn
        int itemsToSpawn = minLootCount[tier];
        if (minLootCount[tier] < maxLootCount[tier])
        {
            itemsToSpawn += world.rand.nextInt(maxLootCount[tier] - minLootCount[tier]);
        }

        //Check if we should care about duplicates
        boolean allowDuplicateEntries = allowDuplicateDrops[tier];

        //Validate data
        if (possibleItems != null && itemsToSpawn > 0)
        {
            //Collect loot to spawn
            final List<LootEntry> lootToSpawn = new ArrayList();

            //Get number of requested items to spawn
            for (int i = 0; i < itemsToSpawn; i++)
            {
                //Loop a few times to get a random entry
                for (int r = 0; r < 6; r++)
                {
                    //Get random entry to allow a chance for all entries to be used
                    LootEntry lootEntry = possibleItems[world.rand.nextInt(possibleItems.length - 1)];

                    //Null check, loaded data can result in nulls in rare cases
                    if (lootEntry != null)
                    {
                        //Random chance
                        if (world.rand.nextFloat() < lootEntry.chanceToDrop
                                //Duplication check
                                && (allowDuplicateEntries || !lootToSpawn.contains(lootEntry)))
                        {
                            lootToSpawn.add(lootEntry);
                            break; //Exit loop
                        }
                    }
                }
            }

            //Drop items
            for (LootEntry lootEntry : lootToSpawn)
            {
                //Get stack, will randomize for ore dictionary
                ItemStack stack = lootEntry.getStack();

                //Can return null for ore dictionary look up
                if (stack != null)
                {
                    //Copy stack to prevent issues
                    stack = stack.copy();

                    //Randomize stack size
                    stack.stackSize = lootEntry.minCount;
                    if (lootEntry.minCount < lootEntry.maxCount)
                    {
                        stack.stackSize += world.rand.nextInt(lootEntry.maxCount - lootEntry.minCount);
                    }

                    //Drop items until stack is empty
                    while (stack != null && stack.stackSize > 0)
                    {
                        //Create
                        EntityItem item = new EntityItem(world);
                        item.setPosition(x + 0.5, y + 0.5, z + 0.5);

                        //Limit stack to stack max size
                        int itemLimit = Math.min(stack.getMaxStackSize(), stack.stackSize);
                        if (itemLimit < stack.stackSize)
                        {
                            ItemStack copy = stack.copy();
                            copy.stackSize = itemLimit;
                            stack.stackSize -= itemLimit;
                            item.setEntityItemStack(copy);
                        }
                        else
                        {
                            item.setEntityItemStack(stack);
                            stack = null;
                        }

                        //Spawn entity
                        world.spawnEntityInWorld(item);
                    }
                }
            }
        }
    }

    /**
     * Loads common settings
     *
     * @param configuration
     */
    public void loadConfiguration(Configuration configuration)
    {
        final String category = "loot_handler";
        lootDataPath = configuration.getString("lootDataPath", category, lootDataPath, "Path to load " +
                "loot table data from. Add './' in front for relative path, or else use the full system path.");
    }

    /**
     * Loads the JSON data from the folder for the loot
     *
     * @param folder
     */
    public void loadLootData(File folder)
    {
        //Get path
        if (lootDataPath.startsWith("./"))
        {
            lootDataFolder = new File(folder, lootDataPath.substring(2, lootDataPath.length()));
        }
        else
        {
            lootDataFolder = new File(lootDataPath);
        }

        //TODO validate path

        //Load if exists
        if (lootDataFolder.exists())
        {
            loadLootData();
        }
        //Generate data if doesn't exist
        else
        {
            lootDataFolder.mkdirs();
            generateDefaultData();
        }
    }

    /**
     * Called to save the loot data to the file system
     */
    public void loadLootData()
    {
        for (int tier = 0; tier < tiers; tier++)
        {
            try
            {
                loadDataFor(tier, getFileForTier(tier));
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Called to save the loot data to the file system
     */
    public void saveLootData()
    {
        for (int tier = 0; tier < tiers; tier++)
        {
            try
            {
                saveDataFor(tier, getFileForTier(tier));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    protected void loadDataFor(int tier, File file) throws IOException
    {
        if (file.exists() && file.isFile())
        {
            //Load data
            FileReader stream = new FileReader(file);
            BufferedReader reader = new BufferedReader(stream);
            JsonReader jsonReader = new JsonReader(reader);
            JsonElement element = Streams.parse(jsonReader);
            stream.close();

            //Parse data
            loadDataFor(tier, element);
        }
    }


    protected void loadDataFor(int tier, JsonElement element)
    {
        if (element.isJsonObject())
        {
            final JsonObject jsonData = element.getAsJsonObject();

            //Validate data
            if (hasKey(jsonData, tier, JSON_MIN_LOOT, "This is required to indicate the min number of loot entries to drop.")
                    && hasKey(jsonData, tier, JSON_MAX_LOOT, "This is required to indicate the max number of loot entries to drop.")
                    && hasKey(jsonData, tier, JSON_LOOT_ARRAY, "This is required to generate items to drop"))
            {
                minLootCount[tier] = Math.max(1, jsonData.getAsJsonPrimitive(JSON_MIN_LOOT).getAsInt());
                maxLootCount[tier] = Math.max(1, jsonData.getAsJsonPrimitive(JSON_MAX_LOOT).getAsInt());

                JsonArray lootEntries = jsonData.getAsJsonArray(JSON_LOOT_ARRAY);
                loot[tier] = new LootEntry[lootEntries.size()];

                int i = 0;
                for (JsonElement lootEntryElement : lootEntries)
                {
                    if (lootEntryElement.isJsonObject())
                    {
                        final JsonObject lootData = lootEntryElement.getAsJsonObject();

                        //Load data
                        String itemName = lootData.get(JSON_ITEM_ID).getAsString();
                        int data = lootData.get(JSON_ITEM_META).getAsInt();
                        int min = lootData.get(JSON_ITEM_MIN_COUNT).getAsInt();
                        int max = lootData.get(JSON_ITEM_MAX_COUNT).getAsInt();
                        float chance = lootData.get(JSON_ITEM_CHANCE).getAsFloat();

                        //Create entry
                        LootEntry lootEntry = null;
                        if (itemName.startsWith("ore@"))
                        {
                            lootEntry = new LootEntry(itemName.substring(4, itemName.length()));
                        }
                        else
                        {
                            Item item = (Item) Item.itemRegistry.getObject(itemName);
                            if (item != null)
                            {
                                lootEntry = new LootEntry(new ItemStack(item, 1, data));
                            }
                            else
                            {
                                Block block = (Block) Block.blockRegistry.getObject(itemName);
                                if (block != null)
                                {
                                    lootEntry = new LootEntry(new ItemStack(block, 1, data));
                                }
                            }
                        }

                        if (lootEntry != null)
                        {
                            //Load data into entry
                            lootEntry.minCount = min;
                            lootEntry.maxCount = max;
                            lootEntry.chanceToDrop = chance;

                            //Add to array
                            loot[tier][i++] = lootEntry;
                        }
                        else
                        {
                            WJLootBoxes.LOGGER.warn("Skipping loot entry for tier " + tier + " loot table data. Failed to locate (item/block/ore) to create entry for '" + itemName + "'.");
                        }
                    }
                }
            }
        }
        else
        {
            WJLootBoxes.LOGGER.error("Failed to load tier " + tier + " loot data due to JSON not being an object." +
                    "Tier loot table data should be nested inside of {} for it to be considered an object");
        }
    }

    protected boolean hasKey(JsonObject jsonData, int tier, String key, String error_message)
    {
        if (!jsonData.has(key))
        {
            WJLootBoxes.LOGGER.error("Failed to load tier " + tier + " loot data due to missing entry [" + key + "]. "
                    + error_message);
            return false;
        }
        return true;
    }

    protected void saveDataFor(int tier, File writeFile) throws IOException
    {
        //Generate JSON for output
        JsonObject object = new JsonObject();
        object.add(JSON_MIN_LOOT, new JsonPrimitive(minLootCount[tier]));
        object.add(JSON_MAX_LOOT, new JsonPrimitive(maxLootCount[tier]));

        JsonArray array = new JsonArray();
        if (loot[tier] != null)
        {
            for (LootEntry lootEntry : loot[tier])
            {
                if (lootEntry != null)
                {
                    JsonObject lootData = new JsonObject();
                    if (lootEntry.oreName != null)
                    {
                        lootData.add(JSON_ITEM_ID, new JsonPrimitive("ore@" + lootEntry.oreName));
                        lootData.add(JSON_ITEM_META, new JsonPrimitive(0));
                    }
                    else
                    {
                        lootData.add(JSON_ITEM_ID, new JsonPrimitive(Item.itemRegistry.getNameForObject(lootEntry.stack.getItem())));
                        lootData.add(JSON_ITEM_META, new JsonPrimitive(lootEntry.stack.getItemDamage()));
                    }

                    lootData.add(JSON_ITEM_MIN_COUNT, new JsonPrimitive(lootEntry.minCount));
                    lootData.add(JSON_ITEM_MAX_COUNT, new JsonPrimitive(lootEntry.maxCount));
                    lootData.add(JSON_ITEM_CHANCE, new JsonPrimitive(lootEntry.chanceToDrop));

                    array.add(lootData);
                }
            }
        }
        object.add(JSON_LOOT_ARRAY, array);


        //Ensure the folder exists
        if (!writeFile.getParentFile().exists())
        {
            writeFile.getParentFile().mkdirs();
        }

        //Write file to disk
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter fileWriter = new FileWriter(writeFile))
        {
            fileWriter.write(gson.toJson(object));
        }
    }

    protected File getFileForTier(int tier)
    {
        return new File(lootDataFolder, "loot_table_tier_" + tier + ".json");
    }

    private void generateDefaultData()
    {
        minLootCount[0] = 1;
        maxLootCount[0] = 3;
        allowDuplicateDrops[0] = true;
        loot[0] = new LootEntry[5];
        loot[0][0] = new LootEntry(new ItemStack(Items.stick), 5, 100, 1);
        loot[0][1] = new LootEntry(new ItemStack(Items.leather_boots), 1, 1, 0.1f);
        loot[0][2] = new LootEntry(new ItemStack(Items.carrot), 5, 10, 0.5f);
        loot[0][3] = new LootEntry(new ItemStack(Items.stone_axe), 1, 2, 0.3f);
        loot[0][4] = new LootEntry(new ItemStack(Items.cooked_beef), 3, 10, 0.1f);


        minLootCount[1] = 1;
        maxLootCount[1] = 5;
        allowDuplicateDrops[1] = true;
        loot[1] = new LootEntry[5];
        loot[1][0] = new LootEntry(new ItemStack(Blocks.stone), 5, 100, 1);
        loot[1][1] = new LootEntry(new ItemStack(Items.leather_chestplate), 1, 1, 0.1f);
        loot[1][2] = new LootEntry(new ItemStack(Items.flint_and_steel), 1, 1, 0.5f);
        loot[1][3] = new LootEntry(new ItemStack(Items.stone_pickaxe), 1, 3, 0.3f);
        loot[1][4] = new LootEntry(new ItemStack(Items.chainmail_helmet), 1, 1, 0.1f);

        minLootCount[2] = 2;
        maxLootCount[2] = 6;
        allowDuplicateDrops[2] = true;
        loot[2] = new LootEntry[5];
        loot[2][0] = new LootEntry(new ItemStack(Blocks.dirt), 5, 100, 1);
        loot[2][1] = new LootEntry(new ItemStack(Items.leather_boots), 1, 1, 0.1f);
        loot[2][2] = new LootEntry(new ItemStack(Blocks.bookshelf), 5, 10, 0.5f);
        loot[2][3] = new LootEntry(new ItemStack(Items.iron_axe), 1, 2, 0.3f);
        loot[2][4] = new LootEntry(new ItemStack(Items.iron_ingot), 3, 10, 0.1f);

        minLootCount[3] = 3;
        maxLootCount[3] = 7;
        allowDuplicateDrops[3] = true;
        loot[3] = new LootEntry[5];
        loot[3][0] = new LootEntry(new ItemStack(Items.stick), 5, 100, 1);
        loot[3][1] = new LootEntry(new ItemStack(Items.leather_boots), 1, 1, 0.1f);
        loot[3][2] = new LootEntry(new ItemStack(Items.carrot), 5, 10, 0.5f);
        loot[3][3] = new LootEntry(new ItemStack(Items.stone_axe), 1, 2, 0.3f);
        loot[3][4] = new LootEntry(new ItemStack(Items.gold_ingot), 3, 10, 0.1f);

        minLootCount[4] = 4;
        maxLootCount[4] = 10;
        allowDuplicateDrops[4] = true;
        loot[4] = new LootEntry[10];
        loot[4][0] = new LootEntry(new ItemStack(Items.flint), 5, 100, 1);
        loot[4][1] = new LootEntry(new ItemStack(Items.diamond_axe), 1, 1, 0.1f);
        loot[4][2] = new LootEntry(new ItemStack(Items.blaze_rod), 5, 10, 0.5f);
        loot[4][3] = new LootEntry(new ItemStack(Items.diamond_boots), 1, 2, 0.3f);
        loot[4][4] = new LootEntry(new ItemStack(Items.diamond_hoe), 1, 2, 0.1f);
        loot[4][5] = new LootEntry(new ItemStack(Items.diamond_horse_armor), 1, 1, 0.1f);
        loot[4][6] = new LootEntry(new ItemStack(Items.diamond_pickaxe), 1, 1, 0.1f);
        loot[4][7] = new LootEntry(new ItemStack(Items.diamond_shovel), 1, 2, 0.5f);
        loot[4][8] = new LootEntry(new ItemStack(Items.diamond_sword), 1, 2, 0.3f);
        loot[4][9] = new LootEntry(new ItemStack(Items.diamond), 3, 10, 0.8f);

        saveLootData();
    }
}
