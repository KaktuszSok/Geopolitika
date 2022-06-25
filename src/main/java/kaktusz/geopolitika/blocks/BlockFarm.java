package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.permaloaded.Farm;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockFarm extends BlockPermaExclusive<Farm> {
	public BlockFarm(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
	}

	@Override
	protected Farm createPermaTE(BlockPos pos) {
		return new Farm(pos);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote)
			return true;

		PermaloadedSavedData save = PermaloadedSavedData.get(worldIn);
		Farm farm = save.getTileEntityAt(pos);
		if(farm == null) {
			worldIn.destroyBlock(pos, true);
			return false;
		}

		farm.setPlantable(playerIn.getHeldItem(EnumHand.MAIN_HAND).getItem());
		return true;
	}
}
