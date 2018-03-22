package com.builtbroken.wjlootboxes.loot;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class LootHandler
{
    /** Array of loot tables for each tier */
    public final LootEntry[][] loot;
    /** Number of items to spawn per tier */
    public final int[] lootCount;
    /** Number of items to spawn per tier */
    public final int[] maxLootCount;
    /** Number of items to spawn per tier */
    public final boolean[] allowDuplicateDrops;

    public LootHandler(int numberOfTiers)
    {
        loot = new LootEntry[numberOfTiers][];
        lootCount = new int[numberOfTiers];
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
        //Get data
        LootEntry[] possibleItems = loot[tier];
        int itemsToSpawn = lootCount[tier] + world.rand.nextInt(maxLootCount[tier]);
        boolean allowDuplicateEntries = allowDuplicateDrops[tier];

        //Validate
        if (possibleItems != null)
        {
            List<LootEntry> lootToSpawn = new ArrayList();

            //Get number of requested items to spawn
            for (int i = 0; i < itemsToSpawn; i++)
            {
                //Loop a few times to get a random entry
                for (int r = 0; r < 6; r++)
                {
                    LootEntry lootEntry = possibleItems[world.rand.nextInt(possibleItems.length - 1)];

                    //Random chance
                    if (world.rand.nextFloat() > lootEntry.chanceToDrop
                            //Duplication check
                            && (allowDuplicateEntries || !lootToSpawn.contains(lootEntry)))
                    {
                        lootToSpawn.add(lootEntry);
                        break; //Exit loop
                    }
                }
            }

            //Drop items
            for (LootEntry lootEntry : lootToSpawn)
            {
                //Copy stack to prevent issues
                ItemStack stack = lootEntry.stack.copy();

                //Randomize stack size
                stack.stackSize = lootEntry.minCount + world.rand.nextInt(lootEntry.maxCount);

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
