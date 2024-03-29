package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.buildings.BuildingHouse;
import kaktusz.geopolitika.integration.PTEDisplay;
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
	public void onLabourTick() {
		if(supplied)
		{
			labourProvidedLastTick = getIdealLabourPerTick() - availableLabour;
			setAvailableLabour(getIdealLabourPerTick());
		}
		else {
			labourProvidedLastTick = 0;
		}
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
	public double getIdealLabourPerTick() {
		return residents*1.0D;
	}

	@Override
	public int getLabourTier() {
		return tier;
	}

	@Override
	public PermaloadedTileEntity getPermaTileEntity() {
		return this;
	}

	@Override
	public PTEDisplay getDisplay() {
		PTEDisplay disp = new PTEDisplay(new ItemStack(Items.BED));
		disp.hoverText = "House";
		if(getIdealLabourPerTick() == 0) {
			if(!wasValidLastRecheck()) {
				disp.hoverText += " (INVALID)";
			}
			else {
				disp.hoverText += " (NO RESIDENTS)";
			}
		} else if(!supplied) {
			disp.hoverText += " (OUT OF SUPPLIES)";
		}
		disp.hoverText += getLabourHoverText(labourProvidedLastTick);
		disp.labourContribution = (float) labourProvidedLastTick;
		disp.idealLabourContribution = (float) getIdealLabourPerTick();
		if(getIdealLabourPerTick() == 0 || !supplied) {
			disp.tint = 0x55FF0000;
			disp.zOrder = 2;
		}
		else if(labourProvidedLastTick >= getIdealLabourPerTick()) {
			disp.tint = 0x55000000;
			disp.zOrder = 0;
		}
		else { //free labour available
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
