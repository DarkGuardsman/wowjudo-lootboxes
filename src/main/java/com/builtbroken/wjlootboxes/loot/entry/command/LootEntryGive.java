package com.builtbroken.wjlootboxes.loot.entry.command;

import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.builtbroken.wjlootboxes.loot.entry.LootEntry;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/7/2018.
 */
public class LootEntryGive extends LootEntry
{
    public String item;
    public int data;
    public String nbt;

    public LootEntryGive(String item, int data, String nbt, int min, int max, float chance)
    {
        super(min, max, chance);
        this.item = item;
        this.data = data;
        this.nbt = nbt;
    }

    public static LootEntryGive newEntry(String itemName, JsonObject lootData)
    {
        int min = lootData.get(LootHandler.JSON_ITEM_MIN_COUNT).getAsInt();
        int max = lootData.get(LootHandler.JSON_ITEM_MAX_COUNT).getAsInt();
        float chance = lootData.get(LootHandler.JSON_ITEM_CHANCE).getAsFloat();

        int data = 0;
        if (lootData.has(LootHandler.JSON_ITEM_DATA))
        {
            data = lootData.get(LootHandler.JSON_ITEM_DATA).getAsInt();
        }

        String nbt = null;
        if (lootData.has(LootHandler.JSON_ITEM_NBT))
        {
            nbt = lootData.get(LootHandler.JSON_ITEM_NBT).getAsString();
        }

        return new LootEntryGive(itemName, data, nbt, min, max, chance);
    }

    @Override
    public void givePlayer(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier)
    {

    }

    @Override
    protected void writeToJson(JsonObject lootData)
    {
        lootData.addProperty(LootHandler.JSON_ITEM_ID, item);
        lootData.addProperty(LootHandler.JSON_ITEM_DATA, data);

        if (nbt != null)
        {
            lootData.addProperty(LootHandler.JSON_ITEM_NBT, nbt);
        }

        super.writeToJson(lootData);
    }

    @Override
    public String toString()
    {
        return "LootEntryGive[" + item + "@" + data + " " + nbt + ", " + minCount + "-" + maxCount + ", " + chanceToDrop + "]@" + hashCode();
    }

}
