package kaktusz.geopolitika.util;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * Cause BlockPos.MutableBlockPos sucks
 */
public class MutableBlockPosition {
	public int x,y,z;

	public MutableBlockPosition(BlockPos pos) {
		x = pos.getX();
		y = pos.getY();
		z = pos.getZ();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MutableBlockPosition that = (MutableBlockPosition) o;
		return x == that.x && y == that.y && z == that.z;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z);
	}

	public BlockPos toBlockPos() {
		return new BlockPos(x,y,z);
	}
}
