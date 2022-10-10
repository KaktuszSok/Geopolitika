package kaktusz.geopolitika.buildings;

import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.BuildingTE;
import kaktusz.geopolitika.permaloaded.tileentities.PermaloadedTileEntity;
import kaktusz.geopolitika.util.BetterToString;
import kaktusz.geopolitika.util.MutableBlockPosition;
import kaktusz.geopolitika.util.RotationUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RoomInfo implements BetterToString {

	public static class CalculationResult<T extends RoomInfo> {
		public final boolean foundOtherBuilding;
		public final T roomInfo;

		public CalculationResult(boolean foundOtherBuilding, @Nullable T roomInfo) {
			this.foundOtherBuilding = foundOtherBuilding;
			this.roomInfo = roomInfo;
		}
	}

	private static final boolean DEBUG_MODE = true;
	private static final boolean DEEP_DEBUG = false;
	private static final int MAX_SIZE_HORIZONTAL = 65;
	private static final int MAX_SIZE_VERTICAL = 64;
	private static final int MAX_VOLUME = MAX_SIZE_HORIZONTAL*MAX_SIZE_VERTICAL*MAX_SIZE_HORIZONTAL/3;

	/**
	 * Accessible floor area in blocks (square metres).
	 */
	protected int floorArea = 0;
	protected final List<BlockPos> possibleConnectedRooms = new ArrayList<>();
	protected final Set<BlockPos> encounteredDoors = new HashSet<>();

	public int getFloorArea() {
		return floorArea;
	}

	public List<BlockPos> getPossibleConnectedRooms() {
		return possibleConnectedRooms;
	}

	public Set<BlockPos> getEncounteredDoors() {
		return encounteredDoors;
	}

	@Nonnull
	public static <T extends RoomInfo> CalculationResult<T> calculateRoom(World world, BlockPos startPos, Set<BlockPos> accessibleBlocksCache, Set<ChunkPos> chunksCache, Supplier<T> roomSupplier, boolean failOnEncounteredExistingBuilding) {

		//0. ensure we are starting on a floor
		BlockPos floorSearchStartBlock = startPos.down();
		if(!isBlockFloor(world, floorSearchStartBlock)) {
			debugLog("No floor at " + floorSearchStartBlock);
			return new CalculationResult<T>(false, null); //failed to find floor
		}

		//1. check if we are in an enclosed area
		Set<BlockPos> checkedBlocks = new HashSet<>();
		Stack<BlockPos> blocksToCheck = new Stack<>(); //TODO make this store unique elements only
		blocksToCheck.push(startPos);

		PermaloadedSavedData permaWorld = PermaloadedSavedData.get(world);
		MutableBlockPosition minimumCorner = new MutableBlockPosition(startPos);
		MutableBlockPosition maximumCorner = new MutableBlockPosition(startPos);
		int volume = 0;
		while (!blocksToCheck.isEmpty()) {
			BlockPos currBlock = blocksToCheck.pop();

			//update bounds and volume and check if we didn't violate the maximum values
			volume++;
			if(volume > MAX_VOLUME) { //room too big
				debugLog("Volume exceeded " + MAX_VOLUME + " at " + currBlock);
				return new CalculationResult<T>(false, null);
			}
			//bounds:
			minimumCorner.x = Math.min(minimumCorner.x, currBlock.getX());
			maximumCorner.x = Math.max(maximumCorner.x, currBlock.getX());
			if(maximumCorner.x - minimumCorner.x > MAX_SIZE_HORIZONTAL) {//room too big
				debugLog("Width exceeded " + MAX_SIZE_HORIZONTAL + " at " + currBlock);
				return new CalculationResult<T>(false, null);
			}
			minimumCorner.y = Math.min(minimumCorner.y, currBlock.getY());
			maximumCorner.y = Math.max(maximumCorner.y, currBlock.getY());
			if(maximumCorner.y - minimumCorner.y > MAX_SIZE_VERTICAL) {//room too big
				debugLog("Height exceeded " + MAX_SIZE_VERTICAL + " at " + currBlock);
				return new CalculationResult<T>(false, null);
			}
			minimumCorner.z = Math.min(minimumCorner.z, currBlock.getZ());
			maximumCorner.z = Math.max(maximumCorner.z, currBlock.getZ());
			if(maximumCorner.z - minimumCorner.z > MAX_SIZE_HORIZONTAL) {//room too big
				debugLog("Width exceeded " + MAX_SIZE_HORIZONTAL + " at " + currBlock);
				return new CalculationResult<T>(false, null);
			}

			//fail if we find another building. In order to avoid failing when finding self, we ignore this when we are on our start block.
			else if(failOnEncounteredExistingBuilding && !floorSearchStartBlock.equals(currBlock) && isBlockOtherBuilding(permaWorld, currBlock)) {
				debugLog("Encountered existing building at " + currBlock + " (floor search start = " + floorSearchStartBlock + ")");
				return new CalculationResult<T>(true, null); //buildings can not share rooms - prioritise existing building.
			}

			//mark block as checked
			checkedBlocks.add(currBlock);

			if(isBlockSolid(world, currBlock))
				continue; //reached a wall - don't add its neighbours

			//push unchecked neighbours to stack
			pushManyToStack(blocksToCheck, checkedBlocks, currBlock.east(), currBlock.west(), currBlock.north(), currBlock.south(), currBlock.down(), currBlock.up());
		}
		int minChunkX = minimumCorner.x >> 4;
		int maxChunkX = maximumCorner.x >> 4;
		int minChunkZ = minimumCorner.z >> 4;
		int maxChunkZ = maximumCorner.z >> 4;
		for (int cx = minChunkX; cx <= maxChunkX; cx++) {
			for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
				chunksCache.add(new ChunkPos(cx, cz)); //keep track of chunks we are occupying (this may be an overestimate e.g. if the room is L-shaped)
			}
		}

		//2. follow floor to calculate room's info (we only consider accessible regions)
		T info = roomSupplier.get();
		blocksToCheck.add(floorSearchStartBlock);
		while (!blocksToCheck.isEmpty()) {
			BlockPos currBlock = blocksToCheck.pop();
			if(!accessibleBlocksCache.add(currBlock))
				continue; //this block was already checked
			BlockPos aboveBlock = currBlock.up();
			boolean checkedAboveBlock = accessibleBlocksCache.contains(aboveBlock);

			//special cases and case 1, floor
			if(processBlock(accessibleBlocksCache, blocksToCheck, info, currBlock, world))
				continue;
			//case 2,3,4: incline, decline or inaccessible
			if(!checkedAboveBlock) { //case 2: incline
				if(processBlock(accessibleBlocksCache, blocksToCheck, info, aboveBlock, world)) {
					accessibleBlocksCache.add(aboveBlock);
					continue;
				}
			}
			BlockPos belowBlock = currBlock.down();
			if(!accessibleBlocksCache.contains(belowBlock)) { //case 3: decline
				if(processBlock(accessibleBlocksCache, blocksToCheck, info, belowBlock, world)) {
					accessibleBlocksCache.add(belowBlock);
					continue;
				}
			}
			//case 4: inaccessible (do nothing - don't add neighbours)
		}
		//remove doors which lead to previous cached coordinates
		info.possibleConnectedRooms.removeIf(accessibleBlocksCache::contains);

		//don't count rooms with no space
		if(info.floorArea == 0) {
			debugLog("Floor area of 0 for room with " + checkedBlocks.size() + " checked blocks and " + accessibleBlocksCache.size() + " accessible blocks");
			return new CalculationResult<T>(false, null);
		}

		return new CalculationResult<T>(false, info);
	}

	private static boolean processBlock(Set<BlockPos> accessibleBlocksCache, Stack<BlockPos> blocksToCheck, RoomInfo info, BlockPos currBlock, World world) {
		deepDebugLog("processing block " + currBlock);
		if(!info.processBlockSpecialCases(world, currBlock)) { //special case: furniture
			deepDebugLog("non-walkable furniture.");
			return true; //furniture was non-walkable
		}

		//special case: door
		if(SpecialBlocks.isBlockDoor(world, currBlock)) {
			EnumFacing facing = RotationUtils.getBlockFacing(world, currBlock);
			BlockPos floorBehindDoor = new BlockPos(
					currBlock.getX() + facing.getXOffset(),
					currBlock.getY() - 1,
					currBlock.getZ() + facing.getZOffset());
			if(accessibleBlocksCache.contains(floorBehindDoor)) { //door was facing towards where we came from - reverse direction
				floorBehindDoor = new BlockPos(
						currBlock.getX() - facing.getXOffset(),
						currBlock.getY() - 1,
						currBlock.getZ() - facing.getZOffset());
			}
			//the door may lead to the same room but that is addressed further down the line
			info.possibleConnectedRooms.add(floorBehindDoor.up());
			info.encounteredDoors.add(currBlock);
			deepDebugLog("door.");
			return true;
		}

		if(isBlockFloor(world, currBlock)) { //case 1: floor
			info.floorArea++;
			pushManyToStack(blocksToCheck, accessibleBlocksCache, currBlock.east(), currBlock.west(), currBlock.north(), currBlock.south());
			deepDebugLog("floor (added neighbours).");
			return true;
		}
		deepDebugLog("inaccessible.");
		return false;
	}

	private static boolean isBlockSolid(World world, BlockPos pos) {
		return world.getBlockState(pos).getMaterial().isSolid();
	}

	private static boolean isBlockFloor(World world, BlockPos pos) {
		if(
			!isBlockSolid(world, pos)
			|| isBlockSolid(world, pos.up()))
		{
			return false; //floor must be solid and block above floor must not be solid
		}
		BlockPos twoAbove = new BlockPos(pos.getX(), pos.getY()+2, pos.getZ());
		boolean hasHeadroom = !isBlockSolid(world, twoAbove); //2 above is not solid

		//special case: bottom slab
		if(!hasHeadroom) { //TODO check how much of a performance impact this step has
			AxisAlignedBB aabb = world.getBlockState(pos).getCollisionBoundingBox(world, pos);
			if(aabb == null)
				return false;
			double top = aabb.maxY;

			AxisAlignedBB aabb2Above = world.getBlockState(twoAbove).getCollisionBoundingBox(world, twoAbove);
			if(aabb2Above == null) {
				return true; //weird but no bounding box means that we certainly have enough headroom
			}
			double bottom = 2+aabb2Above.minY;

			debugLog("headroom: " + top + " to " + bottom + " = " + (bottom-top));

			hasHeadroom = bottom - top >= 1.9d; //require 1.9m between top of floor and bottom of ceiling
		}

		return hasHeadroom;
	}

	private static boolean isBlockOtherBuilding(PermaloadedSavedData permaWorld, BlockPos pos) {
		PermaloadedTileEntity pte = permaWorld.getTileEntityAt(pos);
		if(pte instanceof BuildingTE<?>) {
			return ((BuildingTE<?>)pte).wasValidLastRecheck();
		}
		return false;
	}

	/**
	 * Pushes many elements to the stack, ignoring them if they are in a certain set
	 */
	@SafeVarargs
	private static <T> void pushManyToStack(Stack<T> stack, Set<T> ignore, T... elements) {
		for(T element : elements) {
			if(!ignore.contains(element))
				stack.push(element);
		}
	}

	/**
	 * Checks if the specific block is special (e.g. furniture etc).
	 * @return True if we should treat this block as walkable/a potential floor, false if we should not consider any of its neighbours even if there is enough headroom to walk over it.
	 */
	protected boolean processBlockSpecialCases(World world, BlockPos pos) {
		return true;
	}

	@Override
	public String toString() {
		return toStringInternal();
	}

	@Override
	public void modifyToString(@Nonnull Map<String, Object> propertiesToDisplay) {
		propertiesToDisplay.put("floorArea", floorArea);
		propertiesToDisplay.put("#possibleConnectedRooms", possibleConnectedRooms.size());
	}

	private static void debugLog(String str) {
		if(DEBUG_MODE)
			Geopolitika.logger.info(str);
	}

	private static void deepDebugLog(String str) {
		if(DEEP_DEBUG)
			Geopolitika.logger.info(str);
	}
}
