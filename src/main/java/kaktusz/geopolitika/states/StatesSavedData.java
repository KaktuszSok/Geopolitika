package kaktusz.geopolitika.states;

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
import java.util.HashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StatesSavedData extends WorldSavedData {
	private static final String DATA_NAME = Geopolitika.MODID + "_states";
	private static final String CHUNK_OWNERS_NBT_TAG = "chunkOwners";

	private final Map<ChunkPos, BlockPos> chunkOwners = new HashMap<>();
	private World world;

	public StatesSavedData() {
		super(DATA_NAME);
	}
	public StatesSavedData(String s) {
		super(s);
	}

	public static StatesSavedData get(World world) {
		MapStorage storage = world.getMapStorage();
		StatesSavedData instance = (StatesSavedData) storage.getOrLoadData(StatesSavedData.class, DATA_NAME);

		if(instance == null) {
			instance = new StatesSavedData();
			storage.setData(DATA_NAME, instance);
		}

		instance.world = world;
		return instance;
	}

	public static void set(World world, StatesSavedData data) {
		MapStorage storage = world.getMapStorage();
		storage.setData(DATA_NAME, data);
	}

	public World getWorld() {
		return world;
	}

	public void setChunkControlPoint(ChunkPos chunkPos, BlockPos controlPointPos) {
		chunkOwners.put(chunkPos, controlPointPos);
		markDirty();
	}

	public void removeChunkControlPoint(ChunkPos chunkPos) {
		chunkOwners.remove(chunkPos);
		markDirty();
	}

	@Nullable
	public BlockPos getChunkControlPointPos(ChunkPos chunkPos) {
		return chunkOwners.get(chunkPos);
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
		ModPacketHandler.onStatesDataMarkedDirty(this);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList chunkOwnersNbt = nbt.getTagList(CHUNK_OWNERS_NBT_TAG, 10);
		chunkOwnersNbt.forEach(
				(entryBase) -> {
					NBTTagCompound entry = (NBTTagCompound)entryBase;
					ChunkPos chunkPos = new ChunkPos(entry.getInteger("cx"), entry.getInteger("cz"));
					BlockPos controlPointPos = new BlockPos(entry.getInteger("x"), entry.getShort("y"), entry.getInteger("z"));
					setChunkControlPoint(chunkPos, controlPointPos);
				}
		);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTTagList chunkOwnersNbt = new NBTTagList();
		chunkOwners.forEach(
				(c,cp) -> {
					NBTTagCompound entry = new NBTTagCompound();
					entry.setInteger("cx", c.x);
					entry.setInteger("cz", c.z);
					entry.setInteger("x", cp.getX());
					entry.setShort("y", (short) cp.getY());
					entry.setInteger("z", cp.getZ());
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
			setChunkControlPoint(chunkPos, controlPointPos);
		}
	}

	public void toBytes(ByteBuf buf) {
		buf.writeInt(chunkOwners.size());
		chunkOwners.forEach(
				(c, cp) -> {
					buf.writeInt(c.x);
					buf.writeInt(c.z);
					buf.writeInt(cp.getX());
					buf.writeShort(cp.getY());
					buf.writeInt(cp.getZ());
				}
		);
	}
}
