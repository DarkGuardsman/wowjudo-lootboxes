package com.builtbroken.wjlootboxes;

import com.builtbroken.wjlootboxes.box.BlockLootbox;
import com.builtbroken.wjlootboxes.box.TileEntityLootbox;
import com.builtbroken.wjlootboxes.loot.LootHandler;
import com.builtbroken.wjlootboxes.spawner.BoxSpawner;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.common.config.Configuration;

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

    public static BlockLootbox blockLootbox;

    public static LootHandler lootHandler;
    public static BoxSpawner boxSpawner;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        //Create and register box
        blockLootbox = new BlockLootbox();
        GameRegistry.registerBlock(blockLootbox, "box");
        GameRegistry.registerTileEntity(TileEntityLootbox.class, PREFIX + "box");

        //Load handlers
        lootHandler = new LootHandler(NUMBER_OF_TIERS);
        boxSpawner = new BoxSpawner();

        //Load settings
        Configuration configuration = new Configuration(event.getSuggestedConfigurationFile());
        configuration.load();
        loadConfiguration(configuration);
        configuration.save();
    }

    private void loadConfiguration(Configuration configuration)
    {

    }
}
