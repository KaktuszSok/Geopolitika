package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.Geopolitika;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LabourMachineFE extends LabourMachine<IEnergyStorage> {
	public static final int ID = 900;

	public LabourMachineFE(BlockPos position) {
		super(position);
	}

	@Override
	protected void onLabourNotReceived(TileEntity te, IEnergyStorage capability) {
		Geopolitika.logger.info("Draining " + capability.getEnergyStored() + " energy from " + te.getDisplayName() + " at " + getPosition());
		int drained = capability.receiveEnergy(-capability.getEnergyStored(), false); //using receive to bypass extract limits
		if(drained == 0) //if receiving negative energy does not work, try extract instead
			capability.extractEnergy(capability.getEnergyStored(), false);
	}

	@Override
	protected Capability<IEnergyStorage> getRequiredCapability() {
		return CapabilityEnergy.ENERGY;
	}

	@Override
	public int getID() {
		return ID;
	}
}
