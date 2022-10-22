package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.blocks.BlockPermaBase;
import kaktusz.geopolitika.integration.PTEDisplay;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class CreativeLabourSupplier extends PermaloadedTileEntity implements LabourSupplier, DisplayablePTE {
	public static final int ID = 2001;
	private double availableLabour = 0;
	private double labourProvided = 0;

	public CreativeLabourSupplier(BlockPos position) {
		super(position);
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public double getAvailableLabour() {
		return availableLabour;
	}

	@Override
	public void setAvailableLabour(double labour) {
		availableLabour = labour;
	}

	@Override
	public double getIdealLabourPerTick() {
		return 10;
	}

	@Override
	public int getLabourTier() {
		return 1;
	}

	@Override
	public void onLabourTick() {
		labourProvided = getIdealLabourPerTick() - getAvailableLabour();
		setAvailableLabour(getIdealLabourPerTick());
	}

	@Override
	public PermaloadedTileEntity getPermaTileEntity() {
		return this;
	}

	@Override
	public boolean verify() {
		return getWorld().getBlockState(getPosition()).getBlock() instanceof BlockPermaBase<?>;
	}

	@Nullable
	@Override
	public PTEDisplay getDisplay() {
		PTEDisplay display = new PTEDisplay(new ItemStack(Items.END_CRYSTAL));
		display.hoverText = "Creative Labour Supplier" + getLabourHoverText(labourProvided);
		display.labourContribution = (float) labourProvided;
		display.idealLabourContribution = (float) getIdealLabourPerTick();
		if(labourProvided >= getIdealLabourPerTick()) {
			display.tint = 0x55000000;
			display.zOrder = 0;
		} else {
			display.zOrder = 1;
		}
		return display;
	}
}
