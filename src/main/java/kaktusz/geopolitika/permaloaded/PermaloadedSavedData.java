package kaktusz.geopolitika.permaloaded;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.blocks.BlockPermaBase;
import kaktusz.geopolitika.init.ModConfig;
import kaktusz.geopolitika.permaloaded.tileentities.*;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.Key3;
import kaktusz.geopolitika.util.PrecalcSpiral;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PermaloadedSavedData extends WorldSavedData {
	private static final String DATA_NAME = Geopolitika.MODID + "_permaloadedData";
	public static final Map<Integer, Function<BlockPos, PermaloadedEntity>> entityFactory = new HashMap<>();
	static {
		entityFactory.put(ExclusiveZoneMarker.ID, ExclusiveZoneMarker::new);
		entityFactory.put(ChunkResourcesMarker.ID, ChunkResourcesMarker::new);

		entityFactory.put(LabourMachineFE.ID, LabourMachineFE::new);
		entityFactory.putIfAbsent(ExternalModPTE.ID_MACHINE_GT, bp -> new ExternalModPTE(bp, ExternalModPTE.ID_MACHINE_GT));
	}
	private static final int LABOUR_TICK_RATE = 5; //a labour tick happens every N ticks

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
	@SuppressWarnings("UnstableApiUsage")
	private final Multimap<ChunkPos, LabourSupplier> labourSuppliers = MultimapBuilder.hashKeys().hashSetValues().build();
	@SuppressWarnings("UnstableApiUsage")
	private final Multimap<ChunkPos, LabourConsumer> labourConsumers = MultimapBuilder.hashKeys().hashSetValues().build();
	private final Queue<Runnable> actionsAfterTick = new LinkedList<>();
	private int ticker = 0;

	public PermaloadedSavedData() {
		super(DATA_NAME);
	}
	@SuppressWarnings("unused")
	public PermaloadedSavedData(String name) {
		super(name);
	}

	public static PermaloadedSavedData get(World world) {
		MapStorage storage = world.getMapStorage();
		assert storage != null;
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
		return findTileEntitiesByTypeOrInterface(type, chunk, radius, null, true);
	}

	public <T extends PTEInterface> Collection<T> findTileEntitiesByInterface(Class<T> type, ChunkPos chunk) {
		return findTileEntitiesByInterface(type, chunk, 0);
	}
	public <T extends PTEInterface> Collection<T> findTileEntitiesByInterface(Class<T> type, ChunkPos chunk, int radius) {
		return findTileEntitiesByTypeOrInterface(type, chunk, radius, null, true);
	}

	public <T> List<T> findTileEntitiesByTypeOrInterface(Class<T> type, ChunkPos chunk, int radius, @Nullable final ForgeTeam stateFilter, boolean allowUnclaimedChunks) {
		List<T> found = new ArrayList<>();
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				ChunkPos curr = new ChunkPos(chunk.x + x, chunk.z + z);
				if(stateFilter != null) { //if a state filter is given, skip any chunks claimed by a different state
					ForgeTeam owner = StatesManager.getChunkOwner(curr.x, curr.z, world);
					if(!owner.equalsTeam(stateFilter)) {
						if(owner.isValid() || !allowUnclaimedChunks) {
							continue;
						}
					}
				}
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
	public <T> Iterator<T> iterateTileEntitiesOutwards(Class<T> type, PrecalcSpiral spiral, @Nullable ForgeTeam state) {
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
		removeTileEntityAt(tileEntity.getPosition());

		tileEntity.setSave(this);
		ChunkPos chunkPos = new ChunkPos(tileEntity.getPosition());
		chunkTileEntities.put(chunkPos, tileEntity);
		if(tileEntity instanceof LabourConsumer) {
			labourConsumers.put(chunkPos, (LabourConsumer) tileEntity);
		}
		if(tileEntity instanceof LabourSupplier) {
			labourSuppliers.put(chunkPos, (LabourSupplier) tileEntity);
		}
		if(isNew)
			tileEntity.onAdded();
		tileEntity.onLoaded();
		markDirty();
		return tileEntity;
	}

	public boolean removeTileEntity(PermaloadedTileEntity tileEntity) {
		ChunkPos chunkPos = new ChunkPos(tileEntity.getPosition());
		boolean success = chunkTileEntities.remove(chunkPos, tileEntity);
		if(tileEntity instanceof LabourConsumer) {
			labourConsumers.remove(chunkPos, tileEntity);
		}
		if(tileEntity instanceof LabourSupplier) {
			labourSuppliers.remove(chunkPos, tileEntity);
		}
		tileEntity.onRemoved();
		markDirty();
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

	public Stream<LabourConsumer> getLabourConsumersInChunks(Collection<ChunkPos> chunks) {
		return chunks.stream().flatMap(cp -> labourConsumers.get(cp).stream());
	}

	public Stream<LabourSupplier> getLabourSuppliersInChunks(Collection<ChunkPos> chunks) {
		return chunks.stream().flatMap(cp -> labourSuppliers.get(cp).stream());
	}

	/**
	 * Enqueues an action to be called right after we are done ticking the world.
	 */
	public void queueActionAfterTick(Runnable action) {
		actionsAfterTick.add(action);
	}

	private final PermaloadedTileEntity[] arrayCache = new PermaloadedTileEntity[0];
	public void onChunkLoaded(Chunk chunk) {
		//set resources
		Random rng = chunk.getRandomWithSeed(998);
		Biome biome = chunk.getBiome(chunk.getPos().getBlock(7, 64, 7), world.getBiomeProvider());
		if(rng.nextDouble() <= 1.0/ModConfig.chunkResourcesRarity
				&& !BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN))
		{
			if(ChunkResourcesMarker.getResourcesAt(chunk.getPos(), this) == null) { //don't overwrite persisted resources
				ChunkResourcesMarker resources = addTileEntity(new ChunkResourcesMarker(chunk.getPos()));
				resources.initialise(rng);
				addTileEntity(resources);
			}
		} else {
			removeTileEntityAt(chunk.getPos().getBlock(7, -ChunkResourcesMarker.ID, 7));
		}

		//verify PTEs
		for (PermaloadedTileEntity tileEntity : chunkTileEntities.get(chunk.getPos()).toArray(arrayCache)) {
			if(tileEntity == null)
				continue;

			if(!tileEntity.verify()) {
				removeTileEntity(tileEntity);
			}
		}
	}

	public void onWorldTick() {
		//process labour
		if(ticker % LABOUR_TICK_RATE == 0) {
			labourTick();
		}

		//tick entities
		final PermaloadedTileEntity[] tes = chunkTileEntities.values().toArray(new PermaloadedTileEntity[0]);
		for (PermaloadedTileEntity te : tes) {
			te.onTick();
		}

		//do post-tick actions
		while (!actionsAfterTick.isEmpty()) {
			actionsAfterTick.poll().run();
		}

		ticker++;
	}

	private void labourTick() {
		//tick suppliers
		for (LabourSupplier supplier : labourSuppliers.values()) {
			supplier.onLabourTick();
		}

		//generate chunk-based maps
		//chunkPos, tier, radius -> labour required
		Map<Key3<ChunkPos, Integer, Integer>, Double> labourReqMap = new HashMap<>();
		@SuppressWarnings("UnstableApiUsage")
		Multimap<Key3<ChunkPos, Integer, Integer>, LabourConsumer> requestingConsumersMap = MultimapBuilder.hashKeys().hashSetValues().build();
		HashBasedTable<ChunkPos, Integer, PrecalcSpiral> spirals = HashBasedTable.create();
		for (Map.Entry<ChunkPos, LabourConsumer> kvp : labourConsumers.entries()) {
			Key3<ChunkPos, Integer, Integer> compoundKey = new Key3<>(kvp.getKey(), kvp.getValue().getLabourTier(), kvp.getValue().getSearchRadius());
			kvp.getValue().addLabourReceived(-kvp.getValue().getLabourReceived()); //clear consumer's "stored" labour

			double req = labourReqMap.getOrDefault(compoundKey, 0.0); //get current labour required
			//add labour requirement to map
			req += kvp.getValue().getLabourPerTick();
			labourReqMap.put(compoundKey, req);
			requestingConsumersMap.put(compoundKey, kvp.getValue());
			if(!spirals.contains(compoundKey.a, compoundKey.c)) {
				int length = ((2*compoundKey.c)+1)*((2*compoundKey.c)+1);
				spirals.put(compoundKey.a, compoundKey.c, new PrecalcSpiral(length, compoundKey.a));
			}
		}

		//give labour to chunks
		int step = 0;
		Map<Key3<ChunkPos, Integer, Integer>, Double> labourNeededMap = new HashMap<>(labourReqMap);
		Set<Key3<ChunkPos, Integer, Integer>> needLabour = new HashSet<>(labourNeededMap.keySet());
		while (!needLabour.isEmpty()) {
			final int stepImmutable = step;
			needLabour.removeIf(k3 -> {
				PrecalcSpiral spiral = spirals.get(k3.a, k3.c);
				if(stepImmutable >= spiral.length)
					return true; //remove since we have reached the end of the spiral

				ChunkPos consumeChunk = spiral.positions[stepImmutable];
				ForgeTeam consumeOwner = StatesManager.getChunkOwner(consumeChunk.x, consumeChunk.z, world);
				double needed = labourNeededMap.get(k3);
				if(consumeOwner.isValid() && !StatesManager.getChunkOwner(k3.a.x, k3.a.z, world).equalsTeam(consumeOwner)) { //teams mismatch
					return needed <= 0;
				}

				double received = consumeLabourInChunk(consumeChunk, needed, k3.b);
				needed -= received;
				labourNeededMap.put(k3, needed); //update labour needed map with new (lower) value

				return needed <= 0; //remove if we got all the labour we needed
			});
			step++;
		}

		//distribute labour in chunks to their constituent consumers
		for (Map.Entry<Key3<ChunkPos, Integer, Integer>, Double> kvp : labourReqMap.entrySet()) {
			double labourReceived = kvp.getValue() - labourNeededMap.get(kvp.getKey());

			for (LabourConsumer labourConsumer : requestingConsumersMap.get(kvp.getKey())) {
				if(labourReceived <= 0)
					break;

				double labourForConsumer = Math.min(labourReceived, labourConsumer.getLabourPerTick());
				labourConsumer.addLabourReceived(labourForConsumer);
				labourReceived -= labourForConsumer;
			}
		}
	}

	/**
	 * Consumes labour from suppliers in a given chunk.
	 * @return How much labour could be successfully consumed
	 */
	private double consumeLabourInChunk(ChunkPos chunk, double amount, int tier) {
		Iterator<LabourSupplier> suppliers = labourSuppliers.get(chunk).iterator();

		double received = 0.0;
		while (suppliers.hasNext() && received < amount) {
			received += suppliers.next()
					.requestLabour(amount-received, tier);
		}

		return received;
	}
}
