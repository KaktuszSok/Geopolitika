package kaktusz.geopolitika.states;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import io.netty.buffer.ByteBuf;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.handlers.ModPacketHandler;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChunksSavedData extends WorldSavedData {

	public static class ChunkInfo {
		public BlockPos controlPointPos;
		/**
		 * ID of the state currently controlling this chunk. During an occupation, it is the ID of the special conflict zone state.
		 */
		public short stateId;

		public ChunkInfo(BlockPos controlPointPos, ForgeTeam controllingState) {
			this(controlPointPos, controllingState.getUID());
		}

		public ChunkInfo(BlockPos controlPointPos, short stateId) {
			this.controlPointPos = controlPointPos;
			this.stateId = stateId;
		}
	}

	private static final String DATA_NAME = Geopolitika.MODID + "_stateChunks";
	private static final String CHUNK_OWNERS_NBT_TAG = "chunkOwners";

	private final Map<ChunkPos, ChunkInfo> chunkOwners = new HashMap<>();
	private World world;

	public ChunksSavedData() {
		super(DATA_NAME);
	}
	public ChunksSavedData(String s) {
		super(s);
	}

	public static ChunksSavedData get(World world) {
		MapStorage storage = world.getPerWorldStorage();
		ChunksSavedData instance = (ChunksSavedData) storage.getOrLoadData(ChunksSavedData.class, DATA_NAME);

		if(instance == null) {
			instance = new ChunksSavedData();
			storage.setData(DATA_NAME, instance);
		}

		instance.world = world;
		return instance;
	}

	public static void set(World world, ChunksSavedData data) {
		MapStorage storage = world.getPerWorldStorage();
		storage.setData(DATA_NAME, data);
	}

	public World getWorld() {
		return world;
	}

	public void setChunkControlPoint(ChunkPos chunkPos, BlockPos controlPointPos, short stateId) {
		ChunkInfo info = chunkOwners.get(chunkPos);
		if(info != null) {
			info.controlPointPos = controlPointPos;
			info.stateId = stateId;
		}
		else {
			info = new ChunkInfo(controlPointPos, stateId);
			chunkOwners.put(chunkPos, info);
		}
		markDirty();
	}

	public void removeChunkControlPoint(ChunkPos chunkPos) {
		chunkOwners.remove(chunkPos);
		markDirty();
	}

	@Nullable
	public ChunkInfo getChunkInfo(ChunkPos chunkPos) {
		return chunkOwners.get(chunkPos);
	}

	public Collection<ChunkInfo> getAllChunkInfos() {
		return chunkOwners.values();
	}

	@Nullable
	public BlockPos getChunkControlPointPos(ChunkPos chunkPos) {
		ChunkInfo info = chunkOwners.get(chunkPos);
		if(info == null)
			return null;
		return info.controlPointPos;
	}

	@Nullable
	public TileEntityControlPoint getChunkControlPoint(ChunkPos chunkPos) {
		BlockPos pos = getChunkControlPointPos(chunkPos);
		if(pos == null)
			return null;

		TileEntity te = world.getTileEntity(pos);
		if(!(te instanceof TileEntityControlPoint))
			return null;

		return ((TileEntityControlPoint)te);
	}

	/**
	 * Checks if the given control point position is closer to the chunk than its current control point.
	 * Note: Does not regard the states which control points belong to.
	 * If states are different, the chunk shouldn't be claimed by the new control point.
	 */
	public boolean canControlPointClaimChunk(ChunkPos chunkPos, BlockPos controlPointPos) {
		BlockPos currentClaim = getChunkControlPointPos(chunkPos);
		if(currentClaim == null)
			return true;

		int currentCx = currentClaim.getX() >> 4;
		int currentCz = currentClaim.getZ() >> 4;
		int currentDeltaCx = chunkPos.x - currentCx;
		int currentDeltaCz = chunkPos.z - currentCz;
		int currentDistSq = currentDeltaCx*currentDeltaCx + currentDeltaCz*currentDeltaCz;
		int newCx = controlPointPos.getX() >> 4;
		int newCz = controlPointPos.getZ() >> 4;
		int newDeltaCx = chunkPos.x - newCx;
		int newDeltaCz = chunkPos.z - newCz;
		int newDistSq = newDeltaCx*newDeltaCx + newDeltaCz*newDeltaCz;
		boolean closer = newDistSq < currentDistSq;
		if(!closer && world.isBlockLoaded(currentClaim)) {
			//verify integrity of our data
			TileEntity te = world.getTileEntity(currentClaim);
			if(!(te instanceof TileEntityControlPoint)) {
				return true; //bad data - allow overwrite
			}
		}

		return closer;
	}

	@Override
	public void markDirty() {
		super.markDirty();
		ModPacketHandler.onChunksDataMarkedDirty(this);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList chunkOwnersNbt = nbt.getTagList(CHUNK_OWNERS_NBT_TAG, 10);
		chunkOwnersNbt.forEach(
				(entryBase) -> {
					NBTTagCompound entry = (NBTTagCompound)entryBase;
					ChunkPos chunkPos = new ChunkPos(entry.getInteger("cx"), entry.getInteger("cz"));
					BlockPos controlPointPos = new BlockPos(entry.getInteger("x"), entry.getShort("y"), entry.getInteger("z"));
					setChunkControlPoint(chunkPos, controlPointPos, entry.getShort("state"));
				}
		);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTTagList chunkOwnersNbt = new NBTTagList();
		chunkOwners.forEach(
				(c,info) -> {
					NBTTagCompound entry = new NBTTagCompound();
					entry.setInteger("cx", c.x);
					entry.setInteger("cz", c.z);
					entry.setInteger("x", info.controlPointPos.getX());
					entry.setShort("y", (short) info.controlPointPos.getY());
					entry.setInteger("z", info.controlPointPos.getZ());
					entry.setShort("state", info.stateId);
					chunkOwnersNbt.appendTag(entry);
				}
		);
		nbt.setTag(CHUNK_OWNERS_NBT_TAG, chunkOwnersNbt);

		return nbt;
	}

	public void fromBytes(ByteBuf buf) {
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			ChunkPos chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
			BlockPos controlPointPos = new BlockPos(buf.readInt(), buf.readShort(), buf.readInt());
			setChunkControlPoint(chunkPos, controlPointPos, buf.readShort());
		}
	}

	public void toBytes(ByteBuf buf) {
		buf.writeInt(chunkOwners.size());
		chunkOwners.forEach(
				(c, info) -> {
					buf.writeInt(c.x);
					buf.writeInt(c.z);
					buf.writeInt(info.controlPointPos.getX());
					buf.writeShort(info.controlPointPos.getY());
					buf.writeInt(info.controlPointPos.getZ());
					buf.writeShort(info.stateId);
				}
		);
	}
}
