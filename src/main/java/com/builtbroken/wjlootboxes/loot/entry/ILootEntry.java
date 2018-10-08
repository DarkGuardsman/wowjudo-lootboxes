package com.builtbroken.wjlootboxes.loot.entry;

import com.google.gson.JsonElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/7/2018.
 */
public interface ILootEntry
{
    /**
     * Called to give the loot to the player
     *
     * @param player
     * @param world
     * @param x
     * @param y
     * @param z
     */
    void givePlayer(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier);

    /**
     * Called to see if the entry should be dropped to the player. Called before
     * {@link #givePlayer(EntityPlayer, World, int, int, int, int)} to generate a random
     * list of loot entries from a larger list of possible entries.
     *
     * @param player
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     */
    boolean shouldDrop(@Nullable EntityPlayer player, World world, int x, int y, int z, int tier);

    /**
     * Converts the loot entry to JSON for saving
     *
     * @return element containing save data
     */
    JsonElement toJson();
}
