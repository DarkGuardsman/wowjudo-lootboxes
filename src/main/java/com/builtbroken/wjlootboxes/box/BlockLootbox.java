package com.builtbroken.wjlootboxes.box;

import com.builtbroken.wjlootboxes.WJLootBoxes;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/22/2018.
 */
public class BlockLootbox extends Block
{
    @SideOnly(Side.CLIENT)
    IIcon[] icons;

    public BlockLootbox()
    {
        super(Material.wood);
        setBlockName(WJLootBoxes.PREFIX + "box");
        registerBlockIcons();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta)
    {
        WJLootBoxes.lootHandler.dropRandomLoot(world, x, y, z, meta);
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
        this.blockIcon = reg.registerIcon(this.getTextureName());
    }
}
