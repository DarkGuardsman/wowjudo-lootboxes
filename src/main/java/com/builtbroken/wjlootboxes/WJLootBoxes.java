package com.builtbroken.wjlootboxes;

import com.builtbroken.wjlootboxes.box.BlockLootbox;
import com.builtbroken.wjlootboxes.box.ItemBlockLootbox;
import com.builtbroken.wjlootboxes.box.TileEntityLootbox;
import com.builtbroken.wjlootboxes.command.CommandLootbox;
import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.builtbroken.wjlootboxes.spawner.BoxSpawner;
import com.builtbroken.wjlootboxes.spawner.BoxSpawnerThread;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
@Mod(modid = WJLootBoxes.DOMAIN, name = "WJ Loot Boxes", version = "@MAJOR@.@MINOR@.@REVIS@.@BUILD@")
public class WJLootBoxes
{
    public static final String DOMAIN = "wjlootboxes";
    public static final String PREFIX = DOMAIN + ":";

    public static final int NUMBER_OF_TIERS = 5;

    public static Logger LOGGER;
    public static File configFolder;

    public static BlockLootbox blockLootbox;

    public static LootHandler lootHandler;
    public static BoxSpawner boxSpawner;
    public static BoxSpawnerThread thread;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        //Create logger so we can print errors and info
        LOGGER = LogManager.getLogger("WJ_LootBoxes");

        //Create and register box
        blockLootbox = new BlockLootbox();
        GameRegistry.registerBlock(blockLootbox, ItemBlockLootbox.class, "box");
        GameRegistry.registerTileEntity(TileEntityLootbox.class, PREFIX + "box");

        //Load handlers
        lootHandler = new LootHandler(NUMBER_OF_TIERS);
        boxSpawner = new BoxSpawner();

        FMLCommonHandler.instance().bus().register(boxSpawner);

        //Load settings
        configFolder = new File(event.getModConfigurationDirectory(), DOMAIN);
        loadConfiguration(configFolder);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        //Load data late to allow for blocks to load first
        lootHandler.loadLootData(configFolder);
        boxSpawner.loadSpawnData(configFolder);
    }

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event)
    {
        // Setup command
        ICommandManager commandManager = FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
        ServerCommandManager serverCommandManager = ((ServerCommandManager) commandManager);
        serverCommandManager.registerCommand(new CommandLootbox());

        //Start thread
        thread = new BoxSpawnerThread();
        thread.startScanner();
    }

    @Mod.EventHandler
    public void onServerStop(FMLServerStoppingEvent event)
    {
        thread.kill();
    }

    private void loadConfiguration(File folder)
    {
        File configFile = new File(folder, "main.cfg");
        if (!configFile.getParentFile().exists())
        {
            configFile.getParentFile().mkdirs();
        }

        Configuration configuration = new Configuration(configFile);
        configuration.load();
        lootHandler.loadConfiguration(configuration);
        boxSpawner.loadConfiguration(configuration);
        configuration.save();
    }
}
