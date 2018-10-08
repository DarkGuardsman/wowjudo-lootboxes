package com.builtbroken.wjlootboxes.loot;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import com.builtbroken.wjlootboxes.command.CommandSenderLootbox;
import com.builtbroken.wjlootboxes.loot.entry.ILootEntry;
import com.builtbroken.wjlootboxes.loot.entry.command.LootEntryCommand;
import com.builtbroken.wjlootboxes.loot.entry.command.LootEntryGive;
import com.builtbroken.wjlootboxes.loot.entry.stack.LootEntryItemStack;
import com.builtbroken.wjlootboxes.loot.entry.stack.LootEntryOre;
import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import net.minecraft.command.ICommandManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public static final String JSON_COMMAND = "loot_command";
    public static final String JSON_LOOT_ARRAY = "loot_entries";

    public static final String JSON_ITEM_ID = "item";
    public static final String JSON_ITEM_DATA = "data";
    public static final String JSON_ITEM_NBT = "nbt";
    public static final String JSON_ITEM_MIN_COUNT = "min_count";
    public static final String JSON_ITEM_MAX_COUNT = "max_count";
    public static final String JSON_ITEM_CHANCE = "chance";

    /** Tiers of loot boxes that exist */
    public final int tiers;
    /** Array of loot tables for each tier */
    public final List<ILootEntry>[] loot;
    /** Number of items to spawn per tier */
    public final int[] minLootCount;
    /** Number of items to spawn per tier */
    public final int[] maxLootCount;
    /** Command to run per tier */
    public final String[] commands;
    /** Number of items to spawn per tier */
    public final boolean[] allowDuplicateDrops;

    private String lootDataPath = "./loot";
    private File lootDataFolder;

    public LootHandler(int numberOfTiers)
    {
        tiers = numberOfTiers;
        loot = new List[numberOfTiers];
        minLootCount = new int[numberOfTiers];
        maxLootCount = new int[numberOfTiers];
        allowDuplicateDrops = new boolean[numberOfTiers];
        commands = new String[numberOfTiers];

        for (int i = 0; i < tiers; i++)
        {
            commands[i] = "/wjlootbox loot %player% " + i;
            loot[i] = new ArrayList();
        }
    }

    /**
     * Triggers a chat command linked to the lootbox. Which will then
     * trigger the correct output depending on how the boxes are setup.
     * <p>
     * By default this will call {@link #doDropRandomLoot(EntityPlayer, World, int, int, int, int)}
     * however, can be setup to do anything.
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param tier
     */
    public void onLootDropped(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier)
    {
        if (tier >= 0 && tier < tiers && !world.isRemote)
        {
            MinecraftServer minecraftserver = MinecraftServer.getServer();

            if (minecraftserver != null)
            {
                ICommandManager icommandmanager = minecraftserver.getCommandManager();
                String command = commands[tier];
                if (player != null)
                {
                    command = command.replace("%player%", player.getCommandSenderName());
                }
                else
                {
                    command = command.replace("%player%", "@p[r=3]");
                }
                icommandmanager.executeCommand(new CommandSenderLootbox(world, x, y, z, tier), command);
            }
        }
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
    public void doDropRandomLoot(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier)
    {
        //Get loot to spawn
        List<ILootEntry> possibleItems = loot[tier];

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
            final List<ILootEntry> lootToSpawn = new ArrayList();

            //Get number of requested items to spawn
            for (int i = 0; i < itemsToSpawn; i++)
            {
                //Loop a few times to get a random entry
                for (int r = 0; r < 6; r++)
                {
                    //Get random entry to allow a chance for all entries to be used
                    ILootEntry lootEntry = possibleItems.size() > 1 ? possibleItems.get(world.rand.nextInt(possibleItems.size() - 1)) : possibleItems.get(0);

                    //Null check, loaded data can result in nulls in rare cases
                    if (lootEntry != null)
                    {
                        //Random chance
                        if (lootEntry.shouldDrop(player, world, x, y, z, tier)
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
            for (ILootEntry lootEntry : lootToSpawn)
            {
                lootEntry.givePlayer(player, world, x, y, z, tier);
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
            final JsonObject tierJsonData = element.getAsJsonObject();

            //Validate data
            if (hasKey(tierJsonData, tier, JSON_MIN_LOOT, "This is required to indicate the min number of loot entries to drop.")
                    && hasKey(tierJsonData, tier, JSON_MAX_LOOT, "This is required to indicate the max number of loot entries to drop.")
                    && hasKey(tierJsonData, tier, JSON_LOOT_ARRAY, "This is required to generate items to drop"))
            {
                minLootCount[tier] = Math.max(1, tierJsonData.getAsJsonPrimitive(JSON_MIN_LOOT).getAsInt());
                maxLootCount[tier] = Math.max(1, tierJsonData.getAsJsonPrimitive(JSON_MAX_LOOT).getAsInt());
                if (tierJsonData.has(JSON_COMMAND))
                {
                    commands[tier] = tierJsonData.getAsJsonPrimitive(JSON_COMMAND).getAsString();
                }

                final JsonArray lootEntries = tierJsonData.getAsJsonArray(JSON_LOOT_ARRAY);

                for (JsonElement lootEntryElement : lootEntries)
                {
                    if (lootEntryElement.isJsonObject())
                    {
                        final JsonObject lootJsonData = lootEntryElement.getAsJsonObject();

                        //Load data
                        final String itemName = lootJsonData.get(JSON_ITEM_ID).getAsString();

                        //Create entry
                        ILootEntry lootEntry;
                        if (itemName.startsWith("ore@"))
                        {
                            lootEntry = LootEntryOre.newEntry(itemName.substring(4), lootJsonData);
                        }
                        else if (itemName.startsWith("give@"))
                        {
                            lootEntry = LootEntryGive.newEntry(itemName.substring(5), lootJsonData);
                        }
                        else if (itemName.startsWith("command@"))
                        {
                            lootEntry = LootEntryCommand.newEntry(itemName.substring(8), lootJsonData);
                        }
                        else
                        {
                            lootEntry = LootEntryItemStack.newEntry(itemName, lootJsonData);
                        }

                        if (lootEntry != null)
                        {
                            //Add to array
                            loot[tier].add(lootEntry);
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
        object.addProperty(JSON_MIN_LOOT, minLootCount[tier]);
        object.addProperty(JSON_MAX_LOOT, maxLootCount[tier]);
        object.addProperty(JSON_COMMAND, commands[tier]);

        JsonArray array = new JsonArray();
        if (loot[tier] != null)
        {
            for (ILootEntry lootEntry : loot[tier])
            {
                JsonElement element = saveLootEntry(lootEntry);
                if (element != null)
                {
                    array.add(element);
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

    public static JsonElement saveLootEntry(ILootEntry lootEntry)
    {
        if (lootEntry != null)
        {
            return lootEntry.toJson();
        }
        return null;
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
        loot[0].add(new LootEntryItemStack(new ItemStack(Items.stick), 5, 100, 1));
        loot[0].add(new LootEntryItemStack(new ItemStack(Items.leather_boots), 1, 1, 0.1f));
        loot[0].add(new LootEntryItemStack(new ItemStack(Items.carrot), 5, 10, 0.5f));
        loot[0].add(new LootEntryItemStack(new ItemStack(Items.stone_axe), 1, 2, 0.3f));
        loot[0].add(new LootEntryItemStack(new ItemStack(Items.cooked_beef), 3, 10, 0.1f));


        minLootCount[1] = 1;
        maxLootCount[1] = 5;
        allowDuplicateDrops[1] = true;
        loot[1].add(new LootEntryItemStack(new ItemStack(Blocks.stone), 5, 100, 1));
        loot[1].add(new LootEntryItemStack(new ItemStack(Items.leather_chestplate), 1, 1, 0.1f));
        loot[1].add(new LootEntryItemStack(new ItemStack(Items.flint_and_steel), 1, 1, 0.5f));
        loot[1].add(new LootEntryItemStack(new ItemStack(Items.stone_pickaxe), 1, 3, 0.3f));
        loot[1].add(new LootEntryItemStack(new ItemStack(Items.chainmail_helmet), 1, 1, 0.1f));

        minLootCount[2] = 2;
        maxLootCount[2] = 6;
        allowDuplicateDrops[2] = true;
        loot[2].add(new LootEntryItemStack(new ItemStack(Blocks.dirt), 5, 100, 1));
        loot[2].add(new LootEntryItemStack(new ItemStack(Items.leather_boots), 1, 1, 0.1f));
        loot[2].add(new LootEntryItemStack(new ItemStack(Blocks.bookshelf), 5, 10, 0.5f));
        loot[2].add(new LootEntryItemStack(new ItemStack(Items.iron_axe), 1, 2, 0.3f));
        loot[2].add(new LootEntryItemStack(new ItemStack(Items.iron_ingot), 3, 10, 0.1f));

        minLootCount[3] = 3;
        maxLootCount[3] = 7;
        allowDuplicateDrops[3] = true;
        Enchantment[] aenchantment = Enchantment.enchantmentsList;
        ArrayList<ItemStack> books = new ArrayList();
        for (Enchantment enchantment : aenchantment)
        {
            if (enchantment != null && enchantment.type != null)
            {
                for (int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); ++i)
                {
                    books.add(Items.enchanted_book.getEnchantedItemStack(new EnchantmentData(enchantment, i)));
                }
            }
        }

        loot[3] = books.stream().map(b -> new LootEntryItemStack(b, 1, 3, 0.1f)).collect(Collectors.toList());

        minLootCount[4] = 4;
        maxLootCount[4] = 10;
        allowDuplicateDrops[4] = true;
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.flint), 5, 100, 1));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond_axe), 1, 1, 0.1f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.blaze_rod), 5, 10, 0.5f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond_boots), 1, 2, 0.3f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond_hoe), 1, 2, 0.1f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond_horse_armor), 1, 1, 0.1f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond_pickaxe), 1, 1, 0.1f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond_shovel), 1, 2, 0.5f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond_sword), 1, 2, 0.3f));
        loot[4].add(new LootEntryItemStack(new ItemStack(Items.diamond), 3, 10, 0.8f));

        saveLootData();
    }
}
