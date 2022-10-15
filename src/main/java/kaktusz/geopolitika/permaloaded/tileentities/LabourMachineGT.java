package kaktusz.geopolitika.permaloaded.tileentities;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.permaloaded.ExternalModPTE;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LabourMachineGT extends LabourMachine<IEnergyContainer> {
	public static final int ID = ExternalModPTE.ID_MACHINE_GT;

	public LabourMachineGT(BlockPos position) {
		super(position);
	}

	@Override
	protected void onLabourNotReceived(TileEntity te, IEnergyContainer capability) {
		long in = capability.getInputAmperage()*capability.getInputVoltage();
		long out = capability.getOutputAmperage()*capability.getOutputVoltage();
		long drain = 2*Math.max(in, out);
		Geopolitika.logger.info("Draining " + drain + " energy from " + te.getDisplayName() + " at " + getPosition());
		capability.changeEnergy(-drain); //drain energy at twice the rate it charges/outputs
	}

	@Override
	protected Capability<IEnergyContainer> getRequiredCapability() {
		return GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
	}

	@Override
	public int getID() {
		return ID;
	}
}
