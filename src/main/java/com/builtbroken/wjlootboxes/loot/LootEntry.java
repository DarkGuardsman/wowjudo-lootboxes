package com.builtbroken.wjlootboxes.loot;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

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
        return stack;
    }
}
