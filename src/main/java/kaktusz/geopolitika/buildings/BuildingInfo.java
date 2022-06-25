package kaktusz.geopolitika.buildings;

import kaktusz.geopolitika.Geopolitika;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Supplier;

public class BuildingInfo {
	private static final int MAX_CHUNKS_OCCUPIED = 25;

	public final List<RoomInfo> rooms = new ArrayList<>();

	/**
	 * Calculate a house starting from some position
	 */
	public BuildingInfo(World world, BlockPos startPos) {
		Set<ChunkPos> chunksCache = new HashSet<>();
		Set<BlockPos> accessibleBlocksCache = new HashSet<>();
		Queue<BlockPos> roomCandidates = new LinkedList<>();

		roomCandidates.add(startPos);
		while (!roomCandidates.isEmpty()) {
			if(chunksCache.size() > MAX_CHUNKS_OCCUPIED) {
				rooms.clear();
				return;
			}

			BlockPos roomStartPoint = roomCandidates.poll();
			RoomInfo foundRoom = RoomInfo.calculateRoom(world, roomStartPoint, accessibleBlocksCache, chunksCache, getRoomSupplier());
			accessibleBlocksCache.add(roomStartPoint);
			if(foundRoom == null) {
				continue; //not a room
			}

			rooms.add(foundRoom);
			roomCandidates.addAll(foundRoom.getPossibleConnectedRooms());
		}
	}

	public boolean isValid() {
		return !rooms.isEmpty();
	}

	protected Supplier<RoomInfo> getRoomSupplier() {
		return RoomInfo::new;
	}

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
		return "HouseInfo{" +
				"totalArea=" + getTotalFloorArea() +
				", rooms=" + rooms.toString() +
				'}';
	}
}
