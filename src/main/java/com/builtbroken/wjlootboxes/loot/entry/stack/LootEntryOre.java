package com.builtbroken.wjlootboxes.loot.entry.stack;

import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/7/2018.
 */
public class LootEntryOre extends LootEntryStack
{
    public String oreName;

    public LootEntryOre(String oreName, int min, int max, float chance)
    {
        super(min, max, chance);
        this.oreName = oreName;
    }

    public static LootEntryOre newEntry(String itemName, JsonObject lootData)
    {
        int min = lootData.get(LootHandler.JSON_ITEM_MIN_COUNT).getAsInt();
        int max = lootData.get(LootHandler.JSON_ITEM_MAX_COUNT).getAsInt();
        float chance = lootData.get(LootHandler.JSON_ITEM_CHANCE).getAsFloat();

        return new LootEntryOre(itemName, min, max, chance);
    }

    @Override
    public ItemStack getStack()
    {
        if (oreName != null)
        {
            ArrayList<ItemStack> stacks = OreDictionary.getOres(oreName);
            for (ItemStack stack : stacks)
            {
                if (stack != null && stack.getItem() != null)
                {
                    return stack.copy();
                }
            }
        }
        return null;
    }

    @Override
    protected void writeToJson(JsonObject lootData)
    {
        lootData.add(LootHandler.JSON_ITEM_ID, new JsonPrimitive("ore@" + oreName));
        lootData.add(LootHandler.JSON_ITEM_DATA, new JsonPrimitive(0));
        super.writeToJson(lootData);
    }

    @Override
    public String toString()
    {
        return "LootEntryOre[" + oreName + ", " + minCount + "-" + maxCount + ", " + chanceToDrop + "]@" + hashCode();
    }
}
