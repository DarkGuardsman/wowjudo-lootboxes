package com.builtbroken.wjlootboxes.loot;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * Entry for the loot table
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class LootEntry
{
    public ItemStack stack;
    public String oreName;

    /** Minimal items to drop, should be 1 */
    public int minCount;
    /** Max items to drop, should be less than 10k to prevent lag */
    public int maxCount;

    /** Chance of dropping the item */
    public float chanceToDrop;

    public String originalData;

    public LootEntry(ItemStack stack)
    {
        this.stack = stack;
    }

    public LootEntry(ItemStack stack, int min, int max, float chance)
    {
        this.stack = stack;
        this.minCount = min;
        this.maxCount = max;
        this.chanceToDrop = chance;
    }

    public LootEntry(String ore)
    {
        this.oreName = ore;
    }

    /** Stack to drop */
    public ItemStack getStack()
    {
        if (oreName != null)
        {
            //Get ore values for name
            ArrayList<ItemStack> items = OreDictionary.getOres(oreName);
            if (items != null && items.size() > 0)
            {
                //Try a few times to get an entry
                for (int i = 0; i < 6; i++)
                {
                    //Get random
                    int index = (int) (Math.random() * items.size());
                    ItemStack stack = items.get(index);

                    //Check for null, chance ore values can be null
                    if (stack != null)
                    {
                        return stack.copy(); //Return copy to prevent break ore dictionary
                    }
                }
            }
        }
        if (stack != null)
        {
            return stack.copy();
        }
        return null;
    }

    public void givePlayer(@Nullable EntityPlayer player, World world, int x, int y, int z)
    {
        //Get stack, will randomize for ore dictionary
        ItemStack stack = getStack();

        //Can return null for ore dictionary look up
        if (stack != null && stack.getItem() != null)
        {
            //Randomize stack size
            stack.stackSize = minCount;
            if (minCount < maxCount)
            {
                stack.stackSize += world.rand.nextInt(maxCount - minCount);
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
        else
        {
            WJLootBoxes.LOGGER.error("Received invalid stack from " + this);
        }
    }

    public String toString()
    {
        return "LootEntry[" + originalData + ", " + minCount + "-" + maxCount + ", " + chanceToDrop + "]@" + hashCode();
    }
}
