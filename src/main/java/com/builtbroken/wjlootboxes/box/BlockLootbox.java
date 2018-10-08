package com.builtbroken.wjlootboxes.box;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class BlockLootbox extends Block implements ITileEntityProvider
{
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockLootbox()
    {
        super(Material.wood);
        setBlockName(WJLootBoxes.PREFIX + "box");
        setCreativeTab(CreativeTabs.tabBlock);
        setHardness(1);
        setResistance(1);
    }

    @Override
    public int quantityDropped(Random p_149745_1_)
    {
        return 0;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta)
    {
        if(world.getTileEntity(x, y, z) instanceof TileEntityLootbox)
        {
            WJLootBoxes.lootHandler.onLootDropped(null, world, x, y, z, meta);
        }
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest)
    {
        WJLootBoxes.lootHandler.onLootDropped(player, world, x, y, z, world.getBlockMetadata(x, y, z));
        world.removeTileEntity(x, y, z);
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta)
    {
        return meta < 5 ? icons[meta] : Blocks.wool.getIcon(meta, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg)
    {
        icons = new IIcon[WJLootBoxes.NUMBER_OF_TIERS];
        for (int tier = 0; tier < icons.length; tier++)
        {
            icons[tier] = reg.registerIcon(WJLootBoxes.PREFIX + "box." + tier);
        }
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list)
    {
        for (int tier = 0; tier < icons.length; tier++)
        {
            list.add(new ItemStack(item, 1, tier));
        }
    }

    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        return new TileEntityLootbox();
    }
}
