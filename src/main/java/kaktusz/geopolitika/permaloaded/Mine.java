package kaktusz.geopolitika.permaloaded;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.blocks.BlockMine;
import kaktusz.geopolitika.util.MathsUtils;
import kaktusz.geopolitika.util.PermissionUtils;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.RandomUtils;

import java.util.Random;

public class Mine extends ExclusiveZoneTE {
	public static final int ID = 1001;
	public static final int CHUNK_RADIUS = 3;
	private static final boolean SMOOTH_CORNERS = false;
	private static final int BLOCKS_PER_TICK = 50;
	private static final Random RNG = new Random();

	public Mine(BlockPos position) {
		super(position);
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public int getRadius() {
		return CHUNK_RADIUS;
	}

	@Override
	public boolean verify() {
		return getWorld().getBlockState(getPosition()).getBlock() instanceof BlockMine;
	}

	@Override
	public void onTick() {
		for (int i = 0; i < BLOCKS_PER_TICK; i++) {
			tryMineRandom();
		}
	}

	private void tryMineRandom() {
		int offsetX = MathsUtils.randomRange(-CHUNK_RADIUS, CHUNK_RADIUS+1);
		int offsetZ = MathsUtils.randomRange(-CHUNK_RADIUS, CHUNK_RADIUS+1);
		ChunkPos chosenChunk = new ChunkPos((getPosition().getX() >> 4) + offsetX, (getPosition().getZ() >> 4) + offsetZ);
		int localX = RandomUtils.nextInt(0, 16);
		int localZ = RandomUtils.nextInt(0, 16);
		BlockPos chosenBlock = chosenChunk.getBlock(localX, 255, localZ);
		int dx = chosenBlock.getX() - getPosition().getX();
		int dz = chosenBlock.getZ() - getPosition().getZ();
		if(dx*dx + dz*dz < 5*5)
			return;
		if(SMOOTH_CORNERS) {
			if(offsetX == -CHUNK_RADIUS) {
				if(offsetZ == -CHUNK_RADIUS) {
					if(localX + localZ < 16)
						return;
				} else if(offsetZ == CHUNK_RADIUS) {
					if(localX + 15 - localZ < 16)
						return;
				}
			} else if(offsetX == CHUNK_RADIUS) {
				if(offsetZ == -CHUNK_RADIUS) {
					if(15 - localX + localZ < 16)
						return;
				} else if (offsetZ == CHUNK_RADIUS) {
					if(15 - localX + 15 - localZ < 16)
						return;
				}
			}
		}
		mineBlockBeneath(chosenBlock);
	}

	private void mineBlockBeneath(BlockPos topBlock) {
		if(!getWorld().isBlockLoaded(topBlock))
			return;

		BlockPos chosenBlock = blockcastDown(topBlock);
		if(shouldDestroyBlock(chosenBlock)) {
			if(!PermissionUtils.canBreakBlock(chosenBlock, getWorld())) {
				return;
			}
			getWorld().setBlockState(chosenBlock, Blocks.AIR.getDefaultState(), 2);
		} else {
			//replace stone with random blocks
			if(getWorld().getBlockState(chosenBlock).getBlock() == Blocks.STONE) {
				RNG.setSeed(chosenBlock.toString().hashCode());
				int target = RNG.nextInt(6);
				//0 = keep, 1-5 = replace
				if(target == 0 || !PermissionUtils.canPlaceBlock(chosenBlock, getWorld(), EnumFacing.DOWN)) {
					return;
				}
				IBlockState targetState;
				switch (target) {
					case 1:
						targetState = Blocks.COBBLESTONE.getDefaultState();
						break;
					case 2:
						targetState = Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE);
						break;
					case 3:
						targetState = Blocks.STONE_SLAB.getDefaultState().withProperty(
								BlockStoneSlab.VARIANT,
								BlockStoneSlab.EnumType.COBBLESTONE);
						break;
					case 4:
						targetState = Blocks.STONE_STAIRS.getDefaultState().withProperty(
								BlockStairs.FACING,
								MathsUtils.chooseRandom(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST)
						);
						break;
					case 5:
						if(chosenBlock.getY() < 3)
							return;
						targetState = Blocks.AIR.getDefaultState();
						break;
					default:
						return;
				}
				getWorld().setBlockState(chosenBlock, targetState, 2);
			}
		}
	}

	private boolean shouldDestroyBlock(BlockPos pos) {
		IBlockState state = getWorld().getBlockState(pos);
		return state.getMaterial().isLiquid() || state.getMaterial().isToolNotRequired();
	}

	private BlockPos blockcastDown(BlockPos start) {
		BlockPos.MutableBlockPos current = new BlockPos.MutableBlockPos(start);
		Chunk chunk = getWorld().getChunk(current);

		current.setY(chunk.getTopFilledSegment() + 16);
		for (; current.getY() >= 0; current.setY(current.getY() - 1)) {
			if(!getWorld().isAirBlock(current))
				break;
		}
		return current;
	}
}
