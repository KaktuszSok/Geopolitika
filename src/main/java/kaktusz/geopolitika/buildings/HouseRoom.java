package kaktusz.geopolitika.buildings;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HouseRoom extends RoomInfo {

	private int bedsCount = 0;

	public int getBedsCount() {
		return bedsCount;
	}

	public int getMaxPopulation() {
		return Math.min(bedsCount, getFloorArea()/getMinFloorAreaPerPerson());
	}

	private int getMinFloorAreaPerPerson() {
		return 5;
	}

	@Override
	protected boolean processBlockSpecialCases(World world, BlockPos pos) {

		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if(block instanceof BlockBed) {
			if(!BlockBed.isHeadPiece(block.getMetaFromState(state)))
				bedsCount++;
			return true;
		}

		return true;
	}

	@Override
	public void modifyToString(@Nonnull Map<String, Object> propertiesToDisplay) {
		propertiesToDisplay.put("bedsCount", bedsCount);
		propertiesToDisplay.put("maxPopulation", getMaxPopulation());
		super.modifyToString(propertiesToDisplay);
	}
}
