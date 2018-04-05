package com.builtbroken.wjlootboxes.spawner;

import net.minecraft.world.ChunkPosition;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/4/2018.
 */
public class BoxSpawnerPlacement extends ChunkPosition
{
    public int tier;

    public BoxSpawnerPlacement(int x, int y, int z, int tier)
    {
        super(x, y, z);
        this.tier = tier;
    }
}
