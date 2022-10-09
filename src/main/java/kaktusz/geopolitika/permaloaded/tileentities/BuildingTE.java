package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.blocks.BlockBuilding;
import kaktusz.geopolitika.buildings.BuildingInfo;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class BuildingTE<T extends BuildingInfo<?>> extends PermaloadedTileEntity {

	public static class BuildingStatus<T extends BuildingInfo<?>> {
		public final T buildingInfo;
		public final boolean isSupplied;

		public BuildingStatus(T buildingInfo, boolean isSupplied) {
			this.buildingInfo = buildingInfo;
			this.isSupplied = isSupplied;
		}
	}

	private int nextRecheckTimer = 0;

	public BuildingTE(BlockPos position) {
		super(position);
	}

	@Override
	public boolean verify() {
		return getWorld().getBlockState(getPosition()).getBlock() instanceof BlockBuilding<?>;
	}

	protected abstract T createBuildingInfo(World world, BlockPos pos);

	public BuildingStatus<T> recheckBuilding(boolean consumeSupplies) {
		markDirty();

		//phase 1 - structure
		T buildingInfo = createBuildingInfo(getWorld(), getPosition().up());
		if(!buildingInfo.isValid())
			return new BuildingStatus<>(buildingInfo, false);

		//phase 2 - supplies
		boolean supplied = true;
		if(consumeSupplies) {
			//TODO
		}

		return new BuildingStatus<>(buildingInfo, supplied);
	}

	@Override
	public void onTick() {
		if(nextRecheckTimer > 0) {
			nextRecheckTimer--;
		}
		else {
			if(getWorld().isBlockLoaded(getPosition())) {
				nextRecheckTimer = 20*60*20; //20 minutes
				recheckBuilding(true);
			}
			else {
				nextRecheckTimer = 20*60; //try re-check again next minute since we are unloaded
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setInteger("recheck", nextRecheckTimer);
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		nextRecheckTimer = nbt.getInteger("recheck");
		super.readFromNBT(nbt);
	}
}
