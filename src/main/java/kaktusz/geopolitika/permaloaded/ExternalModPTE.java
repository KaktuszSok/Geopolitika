package kaktusz.geopolitika.permaloaded;

import kaktusz.geopolitika.permaloaded.tileentities.PermaloadedTileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Placeholder for when a PTE loads but the mod which lets us register it is not installed anymore
 */
public class ExternalModPTE extends PermaloadedTileEntity {
	public static int ID_MACHINE_GT = 901;

	public int id;

	public ExternalModPTE(BlockPos position, int id) {
		super(position);
		this.id = id;
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public boolean verify() {
		return true;
	}
}
