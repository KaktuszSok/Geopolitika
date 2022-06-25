package kaktusz.geopolitika.util;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RotationUtils {
	public static EnumFacing getBlockFacing(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		for (IProperty<?> prop : state.getProperties().keySet())
		{
			if ((prop.getName().equals("facing") || prop.getName().equals("rotation")) && prop.getValueClass() == EnumFacing.class)
			{
				@SuppressWarnings("unchecked")
				IProperty<EnumFacing> facingProperty = (IProperty<EnumFacing>) prop;
				return state.getValue(facingProperty);
			}
		}
		return EnumFacing.NORTH;
	}
}
