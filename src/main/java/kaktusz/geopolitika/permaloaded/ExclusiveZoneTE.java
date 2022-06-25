package kaktusz.geopolitika.permaloaded;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;

public abstract class ExclusiveZoneTE extends PermaloadedTileEntity {

	private int savedRadius = 0;

	public ExclusiveZoneTE(BlockPos position) {
		super(position);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		savedRadius = nbt.getByte("claimRadius");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setByte("claimRadius", (byte)savedRadius);
		return compound;
	}

	public abstract int getRadius();

	@Override
	public void onAdded() {
		claimRadius();
	}

	@Override
	public void onRemoved() {
		unclaimRadius();
	}

	public boolean canPlaceHere(World world) {
		ChunkPos centre = new ChunkPos(getPosition());
		Collection<ExclusiveZoneMarker> markers = PermaloadedSavedData.get(world).findTileEntitiesOfType(ExclusiveZoneMarker.class, centre, getRadius());
		return markers.isEmpty();
	}

	protected void claimRadius() {
		int radius = savedRadius = getRadius();
		ChunkPos centre = new ChunkPos(getPosition());
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				ChunkPos curr = new ChunkPos(centre.x + x, centre.z + z);
				ExclusiveZoneMarker marker = new ExclusiveZoneMarker(curr.getBlock(0,999,0));
				getSave().addTileEntity(marker);
			}
		}
	}

	protected void unclaimRadius() {
		ChunkPos centre = new ChunkPos(getPosition());
		Collection<ExclusiveZoneMarker> markers = getSave().findTileEntitiesOfType(ExclusiveZoneMarker.class, centre, getRadius());
		for (ExclusiveZoneMarker marker : markers) {
			getSave().removeTileEntity(marker);
		}
	}
}
