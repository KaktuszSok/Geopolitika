package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.buildings.BuildingInfo;
import kaktusz.geopolitika.permaloaded.tileentities.BuildingTE;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockBuilding<TPerma extends BuildingTE<?>> extends BlockPermaBase<TPerma> {
	private final Function<BlockPos, TPerma> permaTEConstructor;
	public BlockBuilding(String name, Material material, CreativeTabs tab, Function<BlockPos, TPerma> permaTEConstructor) {
		super(name, material, tab);
		this.permaTEConstructor = permaTEConstructor;
	}

	@Override
	protected TPerma createPermaTE(BlockPos pos) {
		return permaTEConstructor.apply(pos);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote)
			return true;

		TPerma permaTE = Objects.requireNonNull(getPermaTileEntity(worldIn, pos));
		BuildingInfo<?> buildingInfo = permaTE.recheckBuilding(true).buildingInfo;
		playerIn.sendMessage(new TextComponentString(buildingInfo.isValid() ? "valid - " + buildingInfo.toString() : "invalid"));
		return true;
	}
}
