package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.util.PrecalcSpiral;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class LabourMachine<T> extends PermaloadedTileEntity implements LabourConsumer, DisplayablePTE {

	private static final Vec3d RAYTRACE_VECTOR = new Vec3d(0, 0, 0);
	private PrecalcSpiral labourSpiral;
	private int reverifyCooldown = 200;
	private double labourReceivedLastTick = 0;
	private String machineName = "Machine";

	public LabourMachine(BlockPos position) {
		super(position);
	}

	@Override
	public void onTick() {
		reverifyCooldown--;
		if(reverifyCooldown <= 0) {
			reverifyCooldown = 200;
			if(!verify()) {
				getSave().queueActionAfterTick(() -> getSave().removeTileEntity(this));
				return;
			}
		}

		labourReceivedLastTick = consumeLabour(getLabourPerTick());
		if(labourReceivedLastTick < getLabourPerTick() && getWorld().isBlockLoaded(getPosition(), false)) {
			TileEntity te = getWorld().getTileEntity(getPosition());
			if(te == null || !te.hasCapability(getRequiredCapability(), null)) {
				Geopolitika.logger.warn("Tile entity with energy expected at " + getPosition() + " but none found.");
				return;
			}
			T cap = te.getCapability(getRequiredCapability(), null);
			assert cap != null;
			onLabourNotReceived(te, cap);
		}
	}

	protected abstract void onLabourNotReceived(TileEntity te, T capability);

	protected abstract Capability<T> getRequiredCapability();

	public void setName(String newName) {
		this.machineName = newName;
		markDirty();
	}

	public double getLabourPerTick() {
		return 0.25D;
	}

	@Override
	public int getLabourTier() {
		return 1;
	}

	@Override
	public int getSearchRadius() {
		return 3;
	}

	@Override
	public PrecalcSpiral getCachedSpiral() {
		return labourSpiral;
	}

	@Override
	public PrecalcSpiral setCachedSpiral(PrecalcSpiral spiral) {
		return labourSpiral = spiral;
	}

	@Override
	public PermaloadedTileEntity getPermaTileEntity() {
		return this;
	}

	@Override
	public boolean verify() {
		TileEntity te = getWorld().getTileEntity(getPosition());
		if(te == null)
			return false;

		return te.hasCapability(getRequiredCapability(), null);
	}

	@Override
	public PTEDisplay getDisplay() {
		ItemStack icon = null;
		if(getWorld().isBlockLoaded(getPosition(), false)) {
			IBlockState blockState = getWorld().getBlockState(getPosition());
			//icon = blockState.getBlock().getItem(getWorld(), getPosition(), blockState);
			icon = blockState.getBlock().getPickBlock(blockState,
					new RayTraceResult(RAYTRACE_VECTOR, EnumFacing.UP, getPosition()),
					getWorld(),
					getPosition(),
					FakePlayerFactory.getMinecraft((WorldServer) getWorld()));
		}

		PTEDisplay display = new PTEDisplay(icon != null && icon.getItem() != Items.AIR ? icon : new ItemStack(Blocks.IRON_BLOCK));
		display.hoverText = machineName + "\n - Labour consumed: " + labourReceivedLastTick + "/" + getLabourPerTick();
		display.labourContribution = (float) -labourReceivedLastTick;
		display.idealLabourContribution = (float) -getLabourPerTick();
		if(labourReceivedLastTick < getLabourPerTick()) {
			display.tint = 0x55FF0000;
			display.zOrder = 2;
		}
		return display;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setString("name", machineName);
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		machineName = nbt.getString("name");
	}
}