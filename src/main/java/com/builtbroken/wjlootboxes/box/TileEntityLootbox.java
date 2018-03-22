package com.builtbroken.wjlootboxes.box;

import net.minecraft.tileentity.TileEntity;

/**
 * Exists purely to track how many tiles are inside the chunk
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class TileEntityLootbox extends TileEntity
{
    @Override
    public boolean canUpdate()
    {
        return false;
    }
}
