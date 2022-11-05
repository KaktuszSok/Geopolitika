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

	/**
	 * Returns how many times "reference" would need to be rotated around its Y axis by 90 degrees to match "compared".
	 */
	public static int getAmountOfYRotationsDelta(EnumFacing reference, EnumFacing compared) {
		if(reference.getAxis() == EnumFacing.Axis.Y || compared.getAxis() == EnumFacing.Axis.Y)
			return 0; //invalid facing

		for (int i = 0; i < 4; i++) {
			if(reference.equals(compared))
				return i;

			reference = reference.rotateY();
		}
		return 0; //should be impossible
	}

	public static IBlockState rotateAroundY(IBlockState blockState, int timesToRotate90deg) {
		for (IProperty<?> prop : blockState.getProperties().keySet())
		{
			if ((prop.getName().equals("facing") || prop.getName().equals("rotation")) && prop.getValueClass() == EnumFacing.class)
			{
//				Block block = blockState.getBlock();
//				if (block instanceof BlockBed || block instanceof BlockPistonExtension) {
//					return blockState;
//				}

				//noinspection unchecked
				IProperty<EnumFacing> facingProperty = (IProperty<EnumFacing>) prop;
				EnumFacing facing = blockState.getValue(facingProperty);
				java.util.Collection<EnumFacing> validFacings = facingProperty.getAllowedValues();

				// rotate horizontal facings clockwise
				EnumFacing newFacing = facing;
				for (int i = 0; i < timesToRotate90deg; i++) {
					newFacing = newFacing.rotateY();
				}
				if(!validFacings.contains(newFacing))
					return blockState;
				return blockState.withProperty(facingProperty, newFacing);
			}
		}
		return blockState;
	}
}
