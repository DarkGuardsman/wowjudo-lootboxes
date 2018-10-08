package com.builtbroken.wjlootboxes.loot.entry;

import com.builtbroken.wjlootboxes.loot.JsonConverterNBT;
import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Entry for the loot table
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class LootEntryItemStack extends LootEntryStack
{
    public ItemStack stack;

    public String originalData;

    public LootEntryItemStack(ItemStack stack, int min, int max, float chance)
    {
        super(min, max, chance);
        this.stack = stack;
    }

    public static LootEntryItemStack newEntry(String itemName, JsonObject lootData)
    {
        int min = lootData.get(LootHandler.JSON_ITEM_MIN_COUNT).getAsInt();
        int max = lootData.get(LootHandler.JSON_ITEM_MAX_COUNT).getAsInt();
        float chance = lootData.get(LootHandler.JSON_ITEM_CHANCE).getAsFloat();

        int itemDamage = lootData.get(LootHandler.JSON_ITEM_DATA).getAsInt();
        Item item = (Item) Item.itemRegistry.getObject(itemName);
        ItemStack stack = null;

        if (item != null)
        {
            stack = new ItemStack(item, 1, itemDamage);
        }
        else
        {
            Block block = (Block) Block.blockRegistry.getObject(itemName);
            if (block != null)
            {
                stack = new ItemStack(block, 1, itemDamage);
            }
        }

        if (stack != null)
        {
            return new LootEntryItemStack(stack, min, max, chance);
        }
        return null;
    }

    @Override
    public ItemStack getStack()
    {
        return stack;
    }

    @Override
    protected void writeToJson(JsonObject lootData)
    {
        lootData.add(LootHandler.JSON_ITEM_ID, new JsonPrimitive(Item.itemRegistry.getNameForObject(stack.getItem())));
        lootData.add(LootHandler.JSON_ITEM_DATA, new JsonPrimitive(stack.getItemDamage()));

        if (stack.getTagCompound() != null && !stack.getTagCompound().hasNoTags())
        {
            lootData.add(LootHandler.JSON_ITEM_NBT, JsonConverterNBT.toJson(stack.getTagCompound()));
        }

        super.writeToJson(lootData);
    }

    @Override
    public String toString()
    {
        return "LootEntry[" + originalData + ", " + minCount + "-" + maxCount + ", " + chanceToDrop + "]@" + hashCode();
    }
}
