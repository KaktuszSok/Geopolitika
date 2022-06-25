package kaktusz.geopolitika.permaloaded;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.permaloaded.projectiles.ProjectileManager;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PermaloadedSavedData extends WorldSavedData {
	private static final String DATA_NAME = Geopolitika.MODID + "_permaloadedData";
	public static final Map<Integer, Function<BlockPos, PermaloadedEntity>> entityFactory = new HashMap<>();
	static {
		entityFactory.put(ExclusiveZoneMarker.ID, ExclusiveZoneMarker::new);
	}

	private World world;
	@SuppressWarnings("UnstableApiUsage")
	private Multimap<ChunkPos, PermaloadedTileEntity> chunkTileEntities = MultimapBuilder.hashKeys().hashSetValues().build();

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
		Collection<T> found = new ArrayList<>();
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				ChunkPos curr = new ChunkPos(chunk.x + x, chunk.z + z);
				for (PermaloadedTileEntity te : chunkTileEntities.get(curr)) {
					if(type.isInstance(te)) {
						//noinspection unchecked
						found.add((T)te);
					}
				}
			}
		}
		return found;
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
