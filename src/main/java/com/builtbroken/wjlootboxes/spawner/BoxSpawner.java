package com.builtbroken.wjlootboxes.spawner;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.HashMap;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class BoxSpawner
{
    public HashMap<Integer, BoxSpawnerWorld> worldToSpawnHandler = new HashMap();
    private String spawnDataPath = "./spawning";
    private File dataFolder;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        BoxSpawnerWorld boxSpawnerWorld = get(event.world);
        if (boxSpawnerWorld != null)
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
        for (String s : worldsAsString)
        {
            try
            {
                int id = Integer.parseInt(s);
                if (!worldToSpawnHandler.containsKey(id))
                {
                    worldToSpawnHandler.put(id, null);
                }
            }
            catch (NumberFormatException e)
            {
                throw new RuntimeException("Failed to parse world id from box_spawner worlds config [" + s + "]");
            }
        }

        spawnDataPath = configuration.getString("spawnDataPath", category, spawnDataPath, "Path to load " +
                "box spawning data from. Add './' in front for relative path, or else use the full system path.");
    }

    public void loadSpawnData(File folder)
    {
        //Get path
        if (spawnDataPath.startsWith("./"))
        {
            dataFolder = new File(folder, spawnDataPath.substring(2, spawnDataPath.length()));
        }
        else
        {
            dataFolder = new File(spawnDataPath);
        }

        //Ensure the folder exists
        if (!dataFolder.exists())
        {
            dataFolder.mkdirs();
        }

        //Build spawn handlers
        for (int i : worldToSpawnHandler.keySet())
        {
            if (worldToSpawnHandler.get(i) == null)
            {
                BoxSpawnerWorld settings = new BoxSpawnerWorld(i);
                worldToSpawnHandler.put(i, settings);
                settings.loadData(getFileForWorld(i));
            }
        }
    }

    protected File getFileForWorld(int tier)
    {
        return new File(dataFolder, "spawn_settings_for_dim_" + tier + ".json");
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
