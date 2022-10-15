package kaktusz.geopolitika.permaloaded.tileentities;

import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.ParticleUtils;
import kaktusz.geopolitika.util.PrecalcSpiral;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

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
				new ChunkPos(getPermaTileEntity().getPosition())
		));
	}

	/**
	 * Consumes labour from nearby suppliers.
	 * @param amount Amount of labour in total which we need to consume.
	 * @return The amount of labour successfully received.
	 */
	default double consumeLabour(double amount) {
		PermaloadedTileEntity permaTE = getPermaTileEntity();

		double received = 0;
		PrecalcSpiral spiral = getOrCreateCachedSpiral();
		Iterator<LabourSupplier> suppliers = permaTE.getSave().iterateTileEntitiesOutwards(
				LabourSupplier.class, new ChunkPos(permaTE.getPosition()), spiral, StatesManager.getChunkOwner(permaTE.getPosition(), permaTE.getWorld()));

		while (received < amount && suppliers.hasNext()) {
			received += suppliers.next().requestLabour(amount-received, getLabourTier());
		}
		return received;
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
