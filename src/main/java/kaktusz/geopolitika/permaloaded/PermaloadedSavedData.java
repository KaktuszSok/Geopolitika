package kaktusz.geopolitika.permaloaded;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.blocks.BlockPermaBase;
import kaktusz.geopolitika.permaloaded.tileentities.*;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.PrecalcSpiral;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PermaloadedSavedData extends WorldSavedData {
	private static final String DATA_NAME = Geopolitika.MODID + "_permaloadedData";
	public static final Map<Integer, Function<BlockPos, PermaloadedEntity>> entityFactory = new HashMap<>();
	static {
		entityFactory.put(ExclusiveZoneMarker.ID, ExclusiveZoneMarker::new);
		entityFactory.put(LabourMachineFE.ID, LabourMachineFE::new);
		entityFactory.putIfAbsent(ExternalModPTE.ID_MACHINE_GT, bp -> new ExternalModPTE(bp, ExternalModPTE.ID_MACHINE_GT));
	}

	/**
	 * Register a permaloaded tile entity block and its permaloaded tile entity.
	 * @return The given block.
	 */
	public static <T extends BlockPermaBase<T2>, T2 extends PermaloadedTileEntity> T registerPTEBlock(T block) {
		block.registerPermaloadedTE();
		return block;
	}

	private World world;
	@SuppressWarnings("UnstableApiUsage")
	private final Multimap<ChunkPos, PermaloadedTileEntity> chunkTileEntities = MultimapBuilder.hashKeys().hashSetValues().build();
	private final Queue<Runnable> actionsAfterTick = new LinkedList<>();

	public PermaloadedSavedData() {
		super(DATA_NAME);
	}
	public PermaloadedSavedData(String name) {
		super(name);
	}

	public static PermaloadedSavedData get(World world) {
		MapStorage storage = world.getMapStorage();
		PermaloadedSavedData instance = (PermaloadedSavedData) storage.getOrLoadData(PermaloadedSavedData.class, DATA_NAME);

		if(instance == null) {
			instance = new PermaloadedSavedData();
			storage.setData(DATA_NAME, instance);
		}

		instance.world = world;
		return instance;
	}

	public World getWorld() {
		return world;
	}

	public <T extends PermaloadedTileEntity> Collection<T> findTileEntitiesOfType(Class<T> type, ChunkPos chunk) {
		return findTileEntitiesOfType(type, chunk, 0);
	}
	public <T extends PermaloadedTileEntity> Collection<T> findTileEntitiesOfType(Class<T> type, ChunkPos chunk, int radius) {
		return findTileEntitiesByTypeOrInterface(type, chunk, radius);
	}

	public <T extends PTEInterface> Collection<T> findTileEntitiesByInterface(Class<T> type, ChunkPos chunk) {
		return findTileEntitiesByInterface(type, chunk, 0);
	}
	public <T extends PTEInterface> Collection<T> findTileEntitiesByInterface(Class<T> type, ChunkPos chunk, int radius) {
		return findTileEntitiesByTypeOrInterface(type, chunk, radius);
	}

	private <T> Collection<T> findTileEntitiesByTypeOrInterface(Class<T> type, ChunkPos chunk, int radius) {
		Collection<T> found = new ArrayList<>();
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				ChunkPos curr = new ChunkPos(chunk.x + x, chunk.z + z);
				for (PermaloadedTileEntity pte : chunkTileEntities.get(curr)) {
					if(type.isInstance(pte)) {
						//noinspection unchecked
						found.add((T)pte);
					}
				}
			}
		}
		return found;
	}

	/**
	 * Checks whether there are any PermaloadedTileEntities of this type within radius chunks of the given chunk.
	 */
	public <T extends PermaloadedTileEntity> boolean hasAnyTileEntitiesOfType(Class<T> type, ChunkPos chunk, int radius) {
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				ChunkPos curr = new ChunkPos(chunk.x + x, chunk.z + z);
				for (PermaloadedTileEntity te : chunkTileEntities.get(curr)) {
					if(type.isInstance(te)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Finds permaloaded tile entities one by one, starting with the centre chunk and moving outwards.
	 * PTEs within the same chunk are ordered arbitrarily.
	 * This iterator should not be persisted between ticks.
	 * @param state If not null, will skip over any chunks owned by a different state than this. If null, will not skip any chunks.
	 */
	public <T> Iterator<T> iterateTileEntitiesOutwards(Class<T> type, ChunkPos chunk, PrecalcSpiral spiral, @Nullable ForgeTeam state) {
		return new Iterator<T>() {
			private int spiralIdx = 0;
			private Iterator<PermaloadedTileEntity> currChunkIter = null;
			private T next = precalcNext();

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public T next() {
				T ret = next;
				next = precalcNext();
				return ret;
			}

			@Nullable
			private T precalcNext() {
				while (spiralIdx < spiral.length) {
					if(currChunkIter == null) {
						currChunkIter = chunkTileEntities.get(spiral.positions[spiralIdx]).iterator();
					}
					while(currChunkIter.hasNext()) {
						PermaloadedTileEntity pte = currChunkIter.next();
						if(type.isInstance(pte)) { //found PTE of valid type!
							//noinspection unchecked
							return (T)pte;
						}
					}
					//finished the current chunk iterator, moving on to the next valid chunk.
					while (true) {
						spiralIdx++;
						if(state == null || spiralIdx >= spiral.length) //only increment once if we don't care about state. Don't increment past the spiral's length.
							break;
						ChunkPos chunkPos = spiral.positions[spiralIdx];
						ForgeTeam owner = StatesManager.getChunkOwner(chunkPos.x, chunkPos.z, world);
						if(!owner.isValid() || owner.equalsTeam(state)) //we found an unclaimed chunk or a chunk owned by our team - stop searching
							break;
					}
					currChunkIter = null;
				}
				return null;
			}
		};
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		chunkTileEntities.clear();
		NBTTagList chunks = nbt.getTagList("chunks", Constants.NBT.TAG_COMPOUND);
		Geopolitika.logger.info("Found data for " + chunks.tagCount() + " chunks");
		for (NBTBase base : chunks) {
			NBTTagCompound chunk = (NBTTagCompound)base;
			ChunkPos cp = new ChunkPos(chunk.getInteger("cx"), chunk.getInteger("cz"));
			NBTTagList chunkTEs = chunk.getTagList("tes", Constants.NBT.TAG_COMPOUND);
			for (NBTBase base2 : chunkTEs) {
				NBTTagCompound teNBT = (NBTTagCompound)base2;
				int id = teNBT.getInteger("id");
				BlockPos tePos = NBTUtil.getPosFromTag(teNBT.getCompoundTag("pos"));
				Function<BlockPos, PermaloadedEntity> supplier = entityFactory.get(id);
				PermaloadedTileEntity te = (PermaloadedTileEntity) supplier.apply(tePos);
				te.readFromNBT(teNBT);
				addTileEntity(te, false);
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagList chunks = new NBTTagList();
		for (ChunkPos cp : chunkTileEntities.keySet()) {
			NBTTagCompound chunkNBT = new NBTTagCompound();
			chunkNBT.setInteger("cx", cp.x);
			chunkNBT.setInteger("cz", cp.z);
			NBTTagList chunkTEs = new NBTTagList();
			for (PermaloadedTileEntity te : chunkTileEntities.get(cp)) {
				if(!te.persistent()) //skip non-persistent PTEs
					continue;

				NBTTagCompound teNBT = new NBTTagCompound();
				teNBT.setInteger("id", te.getID());
				te.writeToNBT(teNBT);
				chunkTEs.appendTag(teNBT);
			}
			chunkNBT.setTag("tes", chunkTEs);

			chunks.appendTag(chunkNBT);
		}
		tag.setTag("chunks", chunks);
		return tag;
	}

	public <T extends PermaloadedTileEntity> T addTileEntity(T tileEntity) {
		return addTileEntity(tileEntity, true);
	}

	/**
	 * @param isNew Is this a newly placed tile entity or are we just loading a pre-existing one?
	 */
	private <T extends PermaloadedTileEntity> T addTileEntity(T tileEntity, boolean isNew) {
		tileEntity.setSave(this);
		chunkTileEntities.put(new ChunkPos(tileEntity.getPosition()), tileEntity);
		if(isNew)
			tileEntity.onAdded();
		tileEntity.onLoaded();
		markDirty();
		Geopolitika.logger.info("Added/loaded TE at " + tileEntity.getPosition());
		return tileEntity;
	}

	public boolean removeTileEntity(PermaloadedTileEntity tileEntity) {
		boolean success = chunkTileEntities.remove(new ChunkPos(tileEntity.getPosition()), tileEntity);
		tileEntity.onRemoved();
		markDirty();
		Geopolitika.logger.info("Removed TE at " + tileEntity.getPosition());
		return success;
	}

	public boolean removeTileEntityAt(BlockPos pos) {
		PermaloadedTileEntity found = null;
		for (PermaloadedTileEntity tileEntity : chunkTileEntities.get(new ChunkPos(pos))) {
			if(tileEntity.getPosition().equals(pos)) {
				found = tileEntity;
				break;
			}
		}
		if(found != null) {
			removeTileEntity(found);
			return true;
		}
		return false;
	}

	/**
	 * Enqueues an action to be called right after we are done ticking the world.
	 */
	public void queueActionAfterTick(Runnable action) {
		actionsAfterTick.add(action);
	}

	private final PermaloadedTileEntity[] arrayCache = new PermaloadedTileEntity[0];
	public void onChunkLoaded(Chunk chunk) {
		for (PermaloadedTileEntity tileEntity : chunkTileEntities.get(chunk.getPos()).toArray(arrayCache)) {
			if(tileEntity == null)
				break;

			if(!tileEntity.verify()) {
				removeTileEntity(tileEntity);
			}
		}
	}

	public void onWorldTick() {
		for (PermaloadedTileEntity te : chunkTileEntities.values()) {
			te.onTick();
		}
		while (!actionsAfterTick.isEmpty()) {
			actionsAfterTick.poll().run();
		}
	}

	@Nullable
	public <T extends PermaloadedTileEntity> T getTileEntityAt(BlockPos pos) {
		for (PermaloadedTileEntity tileEntity : chunkTileEntities.get(new ChunkPos(pos))) {
			if(tileEntity.getPosition().equals(pos)) {
				//noinspection unchecked
				return (T)tileEntity;
			}
		}
		return null;
	}
}
