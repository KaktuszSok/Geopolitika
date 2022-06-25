package kaktusz.geopolitika.buildings;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class SpecialBlocks {
	public static final Set<Block> EXTRA_DOORS = new HashSet<>();

	public static void addDoor(Block doorBlock) {
		EXTRA_DOORS.add(doorBlock);
	}

	public static boolean isBlockDoor(World world, BlockPos pos) {
		Block b = world.getBlockState(pos).getBlock();
		return b instanceof BlockDoor || EXTRA_DOORS.contains(b);
	}
}
