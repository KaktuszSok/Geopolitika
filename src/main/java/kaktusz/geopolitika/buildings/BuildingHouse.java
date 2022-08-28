package kaktusz.geopolitika.buildings;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Supplier;

public class BuildingHouse extends BuildingInfo<HouseRoom> {

	public BuildingHouse(World world, BlockPos startPos) {
		super(world, startPos);
	}

	public int getTotalBeds() {
		return rooms.stream().mapToInt(HouseRoom::getBedsCount).sum();
	}

	public int getMaxTotalPopulation() {
		return rooms.stream().mapToInt(HouseRoom::getMaxPopulation).sum();
	}

	@Override
	protected Supplier<HouseRoom> getRoomSupplier() {
		return HouseRoom::new;
	}

	@Override
	public void modifyToString(@Nonnull Map<String, Object> propertiesToDisplay) {
		propertiesToDisplay.put("totalBeds", getTotalBeds());
		propertiesToDisplay.put("maxTotalPopulation", getMaxTotalPopulation());
		super.modifyToString(propertiesToDisplay);
	}
}
