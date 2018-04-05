package com.builtbroken.wjlootboxes.spawner;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class BoxSpawner
{
    public HashMap<Integer, BoxSpawnerWorld> worldToSpawnHandler = new HashMap();

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        BoxSpawnerWorld boxSpawnerWorld = get(event.world);
        if(boxSpawnerWorld != null)
        {
            boxSpawnerWorld.update(event.world, event.phase);
        }
    }

    public void loadConfiguration(Configuration configuration)
    {
        final String category = "box_spawner";

        //Load worlds
        String[] worldsAsString = configuration.getStringList("worlds", category, new String[]{"0"}, "World to run box spawning inside");

        //Parse dim IDs
        ArrayList<Integer> list = new ArrayList(worldsAsString.length);
        for (String s : worldsAsString)
        {
            try
            {
                int id = Integer.parseInt(s);
                if(!list.contains(id))
                {
                    list.add(id);
                }
            }
            catch (NumberFormatException e)
            {
                throw new RuntimeException("Failed to parse world id from box_spawner worlds config [" + s + "]");
            }
        }

        //Build spawn handlers
        for(int i : list)
        {
            worldToSpawnHandler.put(i, new BoxSpawnerWorld(i));
        }

        //Load data
    }

    public BoxSpawnerWorld get(World world)
    {
        int dim = world.provider.dimensionId;
        if (worldToSpawnHandler.containsKey(dim))
        {
            return worldToSpawnHandler.get(dim);
        }
        return null;
    }
}
