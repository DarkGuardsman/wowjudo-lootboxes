package com.builtbroken.wjlootboxes.command;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import com.builtbroken.wjlootboxes.loot.entry.stack.LootEntryItemStack;
import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.oredict.OreDictionary;

import java.io.File;
import java.io.FileWriter;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 5/6/2018.
 */
public class CommandLootbox extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "wjlootbox";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_)
    {
        return "/wjlootbox";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args == null || args.length == 0 || args[0].equals("?") || args[0].equalsIgnoreCase("help"))
        {
            //TODO print help
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender) + " heldItem    - prints data about the item held"));
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender) + " saveHeld    - saves held item data held to a file for use"));
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender) + " loot <player> <tier>    - spawns random loot for the given tier of lootbox"));
        }
        else if (args[0].equalsIgnoreCase("loot"))
        {
            if (args.length >= 3)
            {
                EntityPlayer player = getPlayer(sender, args[1]);
                int tier = parseInt(sender, args[2]);
                WJLootBoxes.lootHandler.doDropRandomLoot(player, player.worldObj, (int) Math.floor(player.posX), (int) Math.floor(player.posY), (int) Math.floor(player.posZ), tier);
            }
            else
            {
                throw new CommandException("command.wjlootboxes:error.args.missing");
            }
        }
        else if (args[0].equalsIgnoreCase("heldItem"))
        {
            if (sender instanceof EntityPlayer)
            {
                ItemStack stack = ((EntityPlayer) sender).getHeldItem();
                if (stack != null)
                {
                    ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("Item ID: " + Item.itemRegistry.getNameForObject(stack.getItem())));
                    ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("Damage: " + stack.getItemDamage()));
                    ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("Tag: " + stack.getTagCompound()));

                    int[] ids = OreDictionary.getOreIDs(stack);
                    if (ids.length > 0)
                    {
                        ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("Ores:"));
                        for (int id : ids)
                        {
                            ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("   [" + id + "] " + OreDictionary.getOreName(id)));
                        }
                    }
                }
                else
                {
                    ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("You hold nothing"));
                }
            }
            else
            {
                throw new CommandException("command.wjlootboxes:error.player.needed");
            }
        }
        else if (args[0].equalsIgnoreCase("saveHeld"))
        {
            ItemStack stack = ((EntityPlayer) sender).getHeldItem();
            if (stack != null)
            {
                JsonElement element = LootHandler.saveLootEntry(new LootEntryItemStack(stack, 0, 1, 1));

                File file = new File(WJLootBoxes.configFolder, "/items/" + stack.getUnlocalizedName().replace(":", "-").replace("/", "-") + "-" + System.currentTimeMillis() + ".json");
                if (!file.getParentFile().exists())
                {
                    file.getParentFile().mkdirs();
                }

                //Write file to disk
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (FileWriter fileWriter = new FileWriter(file))
                {
                    fileWriter.write(gson.toJson(element));
                    ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("Item data saved as JSON >> " + file.getAbsolutePath()));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("Unexpected error saving file: '" + e.getMessage() + "'"));
                    ((EntityPlayer) sender).addChatComponentMessage(new ChatComponentText("See console for more details"));
                }
            }
        }
    }
}
