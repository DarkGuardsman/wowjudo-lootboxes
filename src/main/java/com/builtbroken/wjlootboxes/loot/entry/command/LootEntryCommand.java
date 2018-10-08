package com.builtbroken.wjlootboxes.loot.entry.command;

import com.builtbroken.wjlootboxes.command.CommandSenderLootbox;
import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.builtbroken.wjlootboxes.loot.entry.ILootEntry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.command.ICommandManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/7/2018.
 */
public class LootEntryCommand implements ILootEntry
{
    public String command;
    public float chanceToDrop;

    public LootEntryCommand(String command, float chanceToDrop)
    {
        this.command = command;
        this.chanceToDrop = chanceToDrop;
    }

    public static LootEntryCommand newEntry(String itemName, JsonObject lootData)
    {
        float chance = lootData.get(LootHandler.JSON_ITEM_CHANCE).getAsFloat();
        return new LootEntryCommand(itemName, chance);
    }

    @Override
    public void givePlayer(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier)
    {
        if (player != null)
        {
            MinecraftServer minecraftserver = MinecraftServer.getServer();

            if (minecraftserver != null)
            {
                ICommandManager icommandmanager = minecraftserver.getCommandManager();
                icommandmanager.executeCommand(new CommandSenderLootbox(world, x, y, z, tier), command);
            }
        }
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
        lootData.addProperty(LootHandler.JSON_ITEM_ID, "command@" + command);
        lootData.addProperty(LootHandler.JSON_ITEM_CHANCE, chanceToDrop);
        return lootData;
    }
}
