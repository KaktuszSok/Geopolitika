package kaktusz.geopolitika.buildings;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

public class BuildingGeneric extends BuildingInfo<RoomInfo> {

	public BuildingGeneric(World world, BlockPos startPos) {
		super(world, startPos);
	}

	@Override
	protected Supplier<RoomInfo> getRoomSupplier() {
		return RoomInfo::new;
	}
}
