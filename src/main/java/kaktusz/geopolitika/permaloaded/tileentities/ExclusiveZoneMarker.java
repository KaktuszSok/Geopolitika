package kaktusz.geopolitika.permaloaded.tileentities;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class ExclusiveZoneMarker extends PermaloadedTileEntity {
	public static final int ID = 999;

	/**
	 * @param position This MUST be 0,999,0 in its chunk-relative coordinates!
	 */
	public ExclusiveZoneMarker(BlockPos position) {
		super(position);
	}

	/**
	 * For marking a chunk as an exclusive zone.
	 */
	public ExclusiveZoneMarker(ChunkPos chunkPos) {
		this(chunkPos.getBlock(0,999,0));
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public boolean persistent() {
		return false;
	}
}
