package kaktusz.geopolitika.buildings;

import kaktusz.geopolitika.util.BetterToString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Supplier;

public abstract class BuildingInfo<T extends RoomInfo> implements BetterToString {
	private static final int MAX_CHUNKS_OCCUPIED = 25;

	public final List<T> rooms = new ArrayList<>();

	/**
	 * Calculate a building starting from some position
	 */
	public BuildingInfo(World world, BlockPos startPos) {
		Set<ChunkPos> chunksCache = new HashSet<>();
		Set<BlockPos> accessibleBlocksCache = new HashSet<>();
		Queue<BlockPos> roomCandidates = new LinkedList<>();

		roomCandidates.add(startPos);
		while (!roomCandidates.isEmpty()) {
			if(chunksCache.size() > MAX_CHUNKS_OCCUPIED) { //explored too many chunks - whole building is invalid.
				rooms.clear();
				return;
			}

			BlockPos roomStartPoint = roomCandidates.poll();
			RoomInfo.CalculationResult<T> foundRoomResult = RoomInfo.calculateRoom(
					world, roomStartPoint,
					accessibleBlocksCache, chunksCache, getRoomSupplier(),
					true);
			if(foundRoomResult.foundOtherBuilding) {
				//found another building while exploring room - this whole building is invalid.
				rooms.clear();
				return;
			}

			accessibleBlocksCache.add(roomStartPoint);
			if(foundRoomResult.roomInfo == null) {
				continue; //not a room
			}

			rooms.add(foundRoomResult.roomInfo);
			roomCandidates.addAll(foundRoomResult.roomInfo.getPossibleConnectedRooms());
		}
	}

	public boolean isValid() {
		return !rooms.isEmpty();
	}

	protected abstract Supplier<T> getRoomSupplier();

	public int getTotalFloorArea() {
		return rooms.stream().mapToInt(RoomInfo::getFloorArea).sum();
	}

	public Set<BlockPos> getAllDoors() {
		Set<BlockPos> doors = new HashSet<>();
		for(RoomInfo room : rooms) {
			doors.addAll(room.getEncounteredDoors());
		}
		return doors;
	}

	@Override
	public String toString() {
		return toStringInternal();
	}

	public void modifyToString(@Nonnull Map<String, Object> propertiesToDisplay) {
		propertiesToDisplay.put("totalArea", getTotalFloorArea());
		propertiesToDisplay.put("rooms", rooms.toString());
	}
}
