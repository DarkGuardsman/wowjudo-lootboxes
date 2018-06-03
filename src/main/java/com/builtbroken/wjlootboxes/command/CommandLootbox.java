package com.builtbroken.wjlootboxes.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.oredict.OreDictionary;

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
                throw new CommandException("That command can only be run from a player");
            }
        }
    }
}
