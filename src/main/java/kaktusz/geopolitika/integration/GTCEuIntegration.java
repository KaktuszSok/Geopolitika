package kaktusz.geopolitika.integration;

import gregtech.api.capability.GregtechCapabilities;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.LabourMachineGT;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.Loader;

public class GTCEuIntegration {
	private static boolean gtceuLoaded = false;

	public static void postInit() {
		gtceuLoaded = Loader.isModLoaded("gregtech");

		if (!gtceuLoaded)
			return;

		PermaloadedSavedData.entityFactory.put(
				LabourMachineGT.ID, LabourMachineGT::new
		);
	}

	public static boolean isGregtechLoaded() {
		return gtceuLoaded;
	}

	public static boolean hasTileEntityGregtechEnergyCapability(TileEntity te) {
		return isGregtechLoaded()
				&& te.hasCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null);
	}
}
