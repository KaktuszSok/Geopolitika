package kaktusz.geopolitika.permaloaded;

import net.minecraft.util.math.BlockPos;

public class ExclusiveZoneMarker extends PermaloadedTileEntity {
	public static final int ID = 999;

	public ExclusiveZoneMarker(BlockPos position) {
		super(position);
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public boolean verify() {
		return true;
	}
}
