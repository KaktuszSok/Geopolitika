package kaktusz.geopolitika.permaloaded;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class PermaloadedEntity {
	private PermaloadedSavedData save = null;

	public PermaloadedSavedData getSave() {
		return save;
	}

	protected void setSave(PermaloadedSavedData save) {
		this.save = save;
	}

	public World getWorld() {
		return save.getWorld();
	}

	/**
	 * Convention:<br>
	 * 0-999: Special Markers<br>
	 * 1XXX: Industry<br>
	 * 2XXX: Buildings<br>
	 * 3XXX: Logistics
	 */
	public abstract int getID();

	/***
	 * Should this entity be saved and loaded?
	 */
	public boolean persistent() {
		return true;
	}

	public void readFromNBT(NBTTagCompound nbt) {

	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		return compound;
	}

	public void markDirty() {
		if(persistent())
			save.markDirty();
	}

	public void onTick() {

	}

	/**
	 * Called when the entity gets added to the world (only called once - not when loaded again)
	 */
	public void onAdded() {

	}

	/**
	 * Called when the entity gets removed from the world
	 */
	public void onRemoved() {

	}

	/**
	 * Called when the entity gets loaded with its world, or if the entity is new and has just been added to the world (right after onAdded)
	 */
	public void onLoaded() {

	}
}
