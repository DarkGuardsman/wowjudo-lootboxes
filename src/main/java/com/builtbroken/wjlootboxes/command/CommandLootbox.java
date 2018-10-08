package com.builtbroken.wjlootboxes.command;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.builtbroken.wjlootboxes.loot.entry.stack.LootEntryItemStack;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.oredict.OreDictionary;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 5/6/2018.
 */
public class CommandLootbox extends CommandBase
{
    private static final String COMMAND_GIVE = "give";
    private static final String COMMAND_LOOT = "loot";
    private static final String COMMAND_ITEM = "heldItem";
    private static final String COMMAND_SAVE = "saveHand";
    private static final String COMMAND_HELP = "help";

    private static final String[] COMMANDS = new String[]{COMMAND_GIVE, COMMAND_LOOT, COMMAND_ITEM, COMMAND_SAVE, COMMAND_HELP};

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
        if (args == null || args.length == 0 || args[0].equals("?") || args[0].equalsIgnoreCase(COMMAND_HELP))
        {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender) + " give    - works the same as the vanilla give command"));
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender) + " heldItem    - prints data about the item held"));
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender) + " saveHeld    - saves held item data held to a file for use"));
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender) + " loot <player> <tier>    - spawns random loot for the given tier of lootbox"));
        }
        else if (args[0].equalsIgnoreCase(COMMAND_GIVE))
        {
            processGive(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        else if (args[0].equalsIgnoreCase(COMMAND_LOOT))
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
        else if (args[0].equalsIgnoreCase(COMMAND_ITEM))
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
        else if (args[0].equalsIgnoreCase(COMMAND_SAVE))
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

    public void processGive(ICommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            throw new WrongUsageException("commands.give.usage", new Object[0]);
        }
        else
        {
            EntityPlayerMP entityplayermp = getPlayer(sender, args[0]);
            Item item = getItemByText(sender, args[1]);
            int i = 1;
            int j = 0;

            if (args.length >= 3)
            {
                i = parseIntBounded(sender, args[2], 1, 64);
            }

            if (args.length >= 4)
            {
                j = parseInt(sender, args[3]);
            }

            ItemStack itemstack = new ItemStack(item, i, j);

            if (args.length >= 5)
            {
                String s = func_147178_a(sender, args, 4).getUnformattedText();

                try
                {
                    NBTBase nbtbase = JsonToNBT.func_150315_a(s);

                    if (!(nbtbase instanceof NBTTagCompound))
                    {
                        func_152373_a(sender, this, "commands.give.tagError", new Object[]{"Not a valid tag"});
                        return;
                    }

                    itemstack.setTagCompound((NBTTagCompound) nbtbase);
                }
                catch (NBTException nbtexception)
                {
                    func_152373_a(sender, this, "commands.give.tagError", new Object[]{nbtexception.getMessage()});
                    return;
                }
            }

            EntityItem entityitem = entityplayermp.dropPlayerItemWithRandomChoice(itemstack, false);
            entityitem.delayBeforeCanPickup = 0;
            entityitem.func_145797_a(entityplayermp.getCommandSenderName());
            //func_152373_a(sender, this, "commands.give.success", new Object[]{itemstack.func_151000_E(), Integer.valueOf(i), entityplayermp.getCommandSenderName()});
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length > 1)
        {
            if (args[0].equalsIgnoreCase(COMMAND_GIVE))
            {
                if (args.length == 2)
                {
                    return getListOfStringsMatchingLastWord(args, this.getPlayers());
                }
                else if (args.length == 3)
                {
                    return getListOfStringsFromIterableMatchingLastWord(args, Item.itemRegistry.getKeys());
                }
            }
            else if (args[0].equalsIgnoreCase(COMMAND_LOOT))
            {
                return getListOfStringsMatchingLastWord(args, this.getPlayers());
            }
        }
        else if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, COMMANDS);
        }
        return null;
    }

    protected String[] getPlayers()
    {
        return MinecraftServer.getServer().getAllUsernames();
    }
}
