package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;

public abstract class ExclusiveZoneTE extends PermaloadedTileEntity {

	public ExclusiveZoneTE(BlockPos position) {
		super(position);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		return compound;
	}

	public abstract int getRadius();

	@Override
	public void onLoaded() {
		claimRadius(); //ExclusiveZoneMarkers are not persistent so we create them whenever we are loaded (or added)
	}

	@Override
	public void onRemoved() {
		unclaimRadius();
	}

	public boolean canPlaceHere(World world) {
		ChunkPos centre = new ChunkPos(getPosition());
		return !PermaloadedSavedData.get(world)
				.hasAnyTileEntitiesOfType(ExclusiveZoneMarker.class, centre, getRadius());
	}

	protected void claimRadius() {
		int radius = getRadius();
		ChunkPos centre = new ChunkPos(getPosition());
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				ChunkPos curr = new ChunkPos(centre.x + x, centre.z + z);
				ExclusiveZoneMarker marker = new ExclusiveZoneMarker(curr);
				getSave().addTileEntity(marker);
			}
		}
	}

	protected void unclaimRadius() {
		ChunkPos centre = new ChunkPos(getPosition());
		Collection<ExclusiveZoneMarker> markers = getSave()
				.findTileEntitiesOfType(ExclusiveZoneMarker.class, centre, getRadius());
		for (ExclusiveZoneMarker marker : markers) {
			getSave().removeTileEntity(marker);
		}
	}
}
