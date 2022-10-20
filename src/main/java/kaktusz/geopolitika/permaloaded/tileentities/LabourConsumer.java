package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.integration.PTEDisplay;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.ParticleUtils;
import kaktusz.geopolitika.util.PrecalcSpiral;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

public interface LabourConsumer extends PTEInterface {

	String NO_LABOUR_TEXT = "\n" + new TextComponentString(" - Labour consumed: 0.0/0.0").setStyle(new Style()
			.setColor(TextFormatting.DARK_GRAY)
	).getFormattedText();
	Style LABOUR_NOT_ENOUGH_STYLE = new Style().setColor(TextFormatting.RED);

	double getLabourPerTick();
	int getLabourTier();

	double getLabourReceived();
	void addLabourReceived(double amount);

	/**
	 * How far the consumer should search for suppliers (in chunks, 0 = only own chunk)
	 */
	int getSearchRadius();

	PrecalcSpiral getCachedSpiral();
	PrecalcSpiral setCachedSpiral(PrecalcSpiral spiral);
	default PrecalcSpiral getOrCreateCachedSpiral() {
		PrecalcSpiral spiral = getCachedSpiral();
		if(spiral != null) {
			return spiral;
		}
		return setCachedSpiral(new PrecalcSpiral(
				((2*getSearchRadius())+1)*((2*getSearchRadius())+1),
				new ChunkPos(getPermaTileEntity().getPosition())
		));
	}

	default PTEDisplay createBasicPTEDisplay(ItemStack icon, String heading) {
		PTEDisplay display = new PTEDisplay(icon);
		display.hoverText = heading + getLabourHoverText();

		boolean enoughLabour = getLabourReceived() >= getLabourPerTick();
		display.labourContribution = enoughLabour ? (float) -getLabourReceived() : 0;
		display.idealLabourContribution = (float) -getLabourPerTick();

		if(!enoughLabour) {
			display.tint = 0x55FF0000;
			display.zOrder = 2;
		}

		return display;
	}

	default String getLabourHoverText() {
		if(getLabourPerTick() == 0) {
			return NO_LABOUR_TEXT;
		}
		String str = " - Labour consumed: " + getLabourReceived() + "/" + getLabourPerTick();
		if(getLabourReceived() < getLabourPerTick()) {
			str = new TextComponentString(str).setStyle(LABOUR_NOT_ENOUGH_STYLE).getFormattedText();
		}

		return "\n" + str;
	}

	/**
	 * Consumes labour from nearby suppliers in a single chunk dictated by the step.
	 * @param step Corresponds to which chunk in our labour spiral we are looking for suppliers in
	 * @return True if done, i.e. we have consumed enough labour or we have reached the end of the labour spiral
	 */
	default boolean consumeLabour(int step) {
		PrecalcSpiral spiral = getOrCreateCachedSpiral();
		if(step >= spiral.length || getLabourReceived() >= getLabourPerTick())
			return true;

		PermaloadedTileEntity permaTE = getPermaTileEntity();

		ChunkPos chunk = spiral.positions[step];
		Iterator<LabourSupplier> suppliers = permaTE.getSave()
				.findTileEntitiesByInterface(LabourSupplier.class, chunk).iterator();

		while (suppliers.hasNext() && getLabourReceived() < getLabourPerTick()) {
			double received = suppliers.next()
					.requestLabour(getLabourPerTick()-getLabourReceived(), getLabourTier());
			addLabourReceived(received);
		}

		return step == spiral.length - 1 || getLabourReceived() >= getLabourPerTick();
	}

	default void spawnLabourNotReceivedParticles() {
		if(ThreadLocalRandom.current().nextBoolean()) //spawn every 2 ticks (on average)
			return;

		BlockPos pos = getPermaTileEntity().getPosition();
		ParticleUtils.spawnParticleForAll(
				(WorldServer) getPermaTileEntity().getWorld(),
				EnumParticleTypes.VILLAGER_ANGRY,
				false,
				pos.getX()+0.5D,
				pos.getY()+0.5d,
				pos.getZ()+0.5D,
				1,
				0.5,
				0.5,
				0.5,
				0.05d
		);
	}
}
