package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.permaloaded.Farm;
import kaktusz.geopolitika.permaloaded.Mine;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockMine extends BlockPermaExclusive<Mine> {
	public BlockMine(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
	}

	@Override
	protected Mine createPermaTE(BlockPos pos) {
		return new Mine(pos);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote)
			return false;

		worldIn.getMinecraftServer().getPlayerList().sendMessage(new TextComponentString("Mine"));
		return false;
	}
}
