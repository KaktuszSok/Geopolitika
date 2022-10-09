package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.PrecalcSpiral;
import net.minecraft.util.math.ChunkPos;

import java.util.Iterator;

public interface LabourConsumer extends PTEInterface {
	int getLabourTier();

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
				new ChunkPos(getTileEntity().getPosition())
		));
	}

	/**
	 * Consumes labour from nearby suppliers.
	 * @param amount Amount of labour in total which we need to consume.
	 * @param tier The tier of labour we are consuming.
	 * @return The amount of labour successfully received.
	 */
	default double consumeLabour(double amount, int tier) {
		PermaloadedTileEntity permaTE = getTileEntity();

		double received = 0;
		PrecalcSpiral spiral = getOrCreateCachedSpiral();
		Iterator<LabourSupplier> suppliers = permaTE.getSave().iterateTileEntitiesOutwards(
				LabourSupplier.class, new ChunkPos(permaTE.getPosition()), spiral, StatesManager.getChunkOwner(permaTE.getPosition(), permaTE.getWorld()));

		while (received < amount && suppliers.hasNext()) {
			received += suppliers.next().requestLabour(amount-received, tier);
		}
		return received;
	}
}
