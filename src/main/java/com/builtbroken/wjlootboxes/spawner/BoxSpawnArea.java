package com.builtbroken.wjlootboxes.spawner;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 5/3/2018.
 */
public class BoxSpawnArea
{
    public final int startX;
    public final int startZ;
    public final int endX;
    public final int endZ;

    public BoxSpawnArea(int startX, int startZ, int endX, int endZ)
    {
        this.startX = startX;
        this.startZ = startZ;
        this.endX = endX;
        this.endZ = endZ;
    }

    public boolean isInside(int x, int z)
    {
        return x <= endX && x >= startX && z <= endZ && z >= startX;
    }

}
