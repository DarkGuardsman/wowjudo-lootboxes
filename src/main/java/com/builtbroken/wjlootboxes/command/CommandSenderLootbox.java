package com.builtbroken.wjlootboxes.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/7/2018.
 */
public class CommandSenderLootbox implements ICommandSender
{
    public final World world;
    public final int x;
    public final int y;
    public final int z;
    public final int tier;

    public CommandSenderLootbox(World world, int x, int y, int z, int tier)
    {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
    }

    @Override
    public String getCommandSenderName()
    {
        return "Lootbox[T: " + tier + " | W: " + world.getProviderName() + " | Pos( " + x + ", " + y + ", " + z + ") ]";
    }

    @Override
    public IChatComponent func_145748_c_()
    {
        return new ChatComponentText(this.getCommandSenderName());
    }

    @Override
    public void addChatMessage(IChatComponent chat)
    {

    }

    @Override
    public boolean canCommandSenderUseCommand(int rank, String string)
    {
        return true;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates()
    {
        return new ChunkCoordinates(x, y, z);
    }

    @Override
    public World getEntityWorld()
    {
        return world;
    }
}
