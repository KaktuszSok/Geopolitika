package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.permaloaded.PermaloadedEntity;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class PermaloadedTileEntity extends PermaloadedEntity {

	public PermaloadedTileEntity(BlockPos position) {
		this.position = position;
	}

	private BlockPos position;

	public BlockPos getPosition() {
		return position;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagCompound pos = nbt.getCompoundTag("pos");
		position = NBTUtil.getPosFromTag(pos);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setTag("pos", NBTUtil.createPosTag(position));
		return compound;
	}

	public abstract boolean verify();
}
