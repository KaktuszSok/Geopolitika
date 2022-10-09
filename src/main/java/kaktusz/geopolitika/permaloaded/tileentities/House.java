package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.buildings.BuildingHouse;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class House extends BuildingTE<BuildingHouse> implements LabourSupplier, DisplayablePTE {
	public static final int ID = 2000;
	private double availableLabour = 0;
	private double labourProvidedLastTick = 0;
	private int residents = 0;
	private int tier = 1;
	private boolean supplied = false;

	public House(BlockPos position) {
		super(position);
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	protected BuildingHouse createBuildingInfo(World world, BlockPos pos) {
		return new BuildingHouse(world, pos);
	}

	@Override
	public BuildingStatus<BuildingHouse> recheckBuilding(boolean consumeSupplies) {
		BuildingStatus<BuildingHouse> result = super.recheckBuilding(consumeSupplies);
		residents = result.buildingInfo.getMaxTotalPopulation();
		tier = 1;
		supplied = result.isSupplied;
		return result;
	}

	@Override
	public void onTick() {
		super.onTick();
		labourProvidedLastTick = getLabourPerTick() - availableLabour;
		setAvailableLabour(getLabourPerTick());
	}

	@Override
	public double getAvailableLabour() {
		return availableLabour;
	}

	@Override
	public void setAvailableLabour(double labour) {
		availableLabour = labour;
	}

	@Override
	public double getLabourPerTick() {
		return residents*1.0D;
	}

	@Override
	public int getLabourTier() {
		return tier;
	}

	@Override
	public PermaloadedTileEntity getTileEntity() {
		return this;
	}

	@Override
	public PTEDisplay getDisplay() {
		PTEDisplay disp = new PTEDisplay(new ItemStack(Items.BED));
		disp.hoverText = "House in de haus\n - Labour provided: " + labourProvidedLastTick + "/" + getLabourPerTick();
		if(getLabourPerTick() == 0) {
			disp.tint = 0x55FF0000;
			disp.zOrder = 2;
		}
		else if(labourProvidedLastTick >= getLabourPerTick()) {
			disp.tint = 0x55000000;
			disp.zOrder = 0;
		}
		else {
			disp.zOrder = 1;
		}
		return disp;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setInteger("residents", residents);
		compound.setInteger("tier", tier);
		compound.setBoolean("supplied", supplied);
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		residents = nbt.getInteger("residents");
		tier = nbt.getInteger("tier");
		supplied = nbt.getBoolean("supplied");
		super.readFromNBT(nbt);
	}
}
