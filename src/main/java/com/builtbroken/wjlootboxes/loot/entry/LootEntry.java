package com.builtbroken.wjlootboxes.loot.entry;

import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/7/2018.
 */
public abstract class LootEntry implements ILootEntry
{
    /** Minimal items to drop, should be 1 */
    public int minCount;
    /** Max items to drop, should be less than 10k to prevent lag */
    public int maxCount;

    /** Chance of dropping the item */
    public float chanceToDrop;

    public LootEntry(int min, int max, float chance)
    {
        this.minCount = min;
        this.maxCount = max;
        this.chanceToDrop = chance;
    }

    @Override
    public boolean shouldDrop(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier)
    {
        return world.rand.nextFloat() < chanceToDrop;
    }

    @Override
    public JsonElement toJson()
    {
        JsonObject lootData = new JsonObject();
        writeToJson(lootData);
        return lootData;
    }

    protected void writeToJson(JsonObject lootData)
    {
        lootData.add(LootHandler.JSON_ITEM_MIN_COUNT, new JsonPrimitive(minCount));
        lootData.add(LootHandler.JSON_ITEM_MAX_COUNT, new JsonPrimitive(maxCount));
        lootData.add(LootHandler.JSON_ITEM_CHANCE, new JsonPrimitive(chanceToDrop));
    }
}
