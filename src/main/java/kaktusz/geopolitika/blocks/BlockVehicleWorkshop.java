package kaktusz.geopolitika.blocks;

import kaktusz.geopolitika.entities.EntityCustomVehicle;
import kaktusz.geopolitika.util.RotationUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockVehicleWorkshop extends BlockHorizontalBase {

	private final int halfSizeX, sizeY, sizeZ;

	public BlockVehicleWorkshop(String name, Material material, CreativeTabs tab, int halfSizeX, int sizeY, int sizeZ) {
		super(name, material, tab);
		this.halfSizeX = halfSizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote)
			return true;

		EnumFacing blockFacing = state.getValue(BlockHorizontalBase.FACING);
		Map<Vec3i, IBlockState> states = new HashMap<>();
		int centreZ = (sizeZ+1)/2;
		int workshopRotation = RotationUtils.getAmountOfYRotationsDelta(EnumFacing.NORTH, blockFacing);
		for (int right = -halfSizeX; right <= halfSizeX; right++) {
			for (int back = 1; back <= sizeZ; back++) {
				for (int up = 1; up <= sizeY; up++) {
					IBlockState scannedState = getStateAt(worldIn, pos, blockFacing, right, up, back);
					scannedState = RotationUtils.rotateAroundY(scannedState, workshopRotation);
					Vec3i localPos = new Vec3i(right, up-1, centreZ-back);

					if(isStateValid(scannedState))
						states.put(localPos, scannedState);
					if(playerIn.isSneaking())
						worldIn.setBlockState(getRelativePos(pos, blockFacing, right, up, back), Blocks.GRAVEL.getDefaultState(), 2);
				}
			}
		}
		playerIn.sendMessage(new TextComponentString("Found " + states.size() + " valid blocks in workshop volume."));

		EntityCustomVehicle vehicle = new EntityCustomVehicle(worldIn, states);
		vehicle.setLocationAndAngles(
				pos.getX() + 0.5d + blockFacing.getXOffset()*sizeZ,
				pos.getY(),
				pos.getZ() + 0.25d + blockFacing.getZOffset()*sizeZ,
				20f,
				37f
				);
		vehicle.addVelocity(0.1, 1, 1.5);
		worldIn.spawnEntity(vehicle);

		return true;
	}

	protected boolean isStateValid(IBlockState state) {
		if(state.getMaterial() == Material.AIR)
			return false;

		return true;
	}

	/**
	 * Gets the block state relative to a workshop block
	 * @param world World containing the blocks to find
	 * @param thisPos Position of the workshop block
	 * @param thisFacing Direction the workshop block is facing
	 * @param offsetR Offset to the right of the workshop block (as seen when facing it head-on)
	 * @param offsetU offset upwards from the workshop block
	 * @param offsetB offset to the back of the workshop block (assuming the front of the vehicle is at offset 1)
	 * @return The block at the resulting position
	 */
	protected IBlockState getStateAt(World world, BlockPos thisPos, EnumFacing thisFacing, int offsetR, int offsetU, int offsetB) {
		BlockPos truePos = getRelativePos(thisPos, thisFacing, offsetR, offsetU, offsetB);
		return world.getBlockState(truePos);
	}

	/**
	 * Gets the block position relative to a workshop block
	 * @param thisPos Position of the workshop block
	 * @param thisFacing Direction the workshop block is facing
	 * @param offsetR Offset to the right of the workshop block (as seen when facing it head-on)
	 * @param offsetU offset upwards from the workshop block
	 * @param offsetB offset to the back of the workshop block (assuming the front of the vehicle is at offset 1)
	 * @return The resulting position
	 */
	protected BlockPos getRelativePos(BlockPos thisPos, EnumFacing thisFacing, int offsetR, int offsetU, int offsetB) {
		Vec3i right = thisFacing.rotateYCCW().getDirectionVec();
		Vec3i back = thisFacing.getOpposite().getDirectionVec();
		int offsetX = right.getX()*offsetR + back.getX()*offsetB;
		int offsetZ = right.getZ()*offsetR + back.getZ()*offsetB;
		return new BlockPos(thisPos.getX() + offsetX, thisPos.getY() + offsetU, thisPos.getZ() + offsetZ);
	}
}
