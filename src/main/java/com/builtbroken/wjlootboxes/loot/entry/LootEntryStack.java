package com.builtbroken.wjlootboxes.loot.entry;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/7/2018.
 */
public abstract class LootEntryStack extends LootEntry
{
    public LootEntryStack(int min, int max, float chance)
    {
        super(min, max, chance);
    }

    /** Stack to drop */
    public abstract ItemStack getStack();

    @Override
    public void givePlayer(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier)
    {
        //Get stack, will randomize for ore dictionary
        ItemStack stack = getStack();

        //Can return null for ore dictionary look up
        if (stack != null && stack.getItem() != null)
        {
            //Sets the stack size of the dropped stack
            getStackDrop(player, stack, world, x, y, z);

            //Give item
            giveStack(player, world, x, y, z, stack);
        }
        else
        {
            WJLootBoxes.LOGGER.error("Received invalid stack from " + this);
        }
    }

    protected void getStackDrop(@Nullable EntityPlayer player, ItemStack stack, World world, int x, int y, int z)
    {
        //Randomize stack size
        stack.stackSize = minCount;
        if (minCount < maxCount)
        {
            stack.stackSize += world.rand.nextInt(maxCount - minCount);
        }
    }

    protected void giveStack(@Nullable EntityPlayer player, World world, int x, int y, int z, ItemStack stack)
    {
        //Drop items until stack is empty
        while (stack != null && stack.stackSize > 0)
        {
            //Limit stack to stack max size
            int itemLimit = Math.min(stack.getMaxStackSize(), stack.stackSize);
            if (itemLimit < stack.stackSize)
            {
                ItemStack copy = stack.copy();
                copy.stackSize = itemLimit;
                stack.stackSize -= itemLimit;
                if (!player.inventory.addItemStackToInventory(copy))
                {
                    dropStack(world, x, y, z, copy);
                }
            }
            else
            {
                if (!player.inventory.addItemStackToInventory(stack))
                {
                    dropStack(world, x, y, z, stack);
                }
                stack = null;
            }
        }
    }

    protected void dropStack(World world, int x, int y, int z, ItemStack stack)
    {
        //Create
        EntityItem item = new EntityItem(world);
        item.setPosition(x + 0.5, y + 0.5, z + 0.5);
        item.setEntityItemStack(stack);
        //Spawn entity
        world.spawnEntityInWorld(item);
    }
}
