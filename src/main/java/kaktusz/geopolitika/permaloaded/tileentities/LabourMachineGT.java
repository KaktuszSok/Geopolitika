package kaktusz.geopolitika.permaloaded.tileentities;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
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
		capability.changeEnergy(-2*capability.getInputVoltage()*capability.getInputAmperage()); //drain energy at twice the rate it charges
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
