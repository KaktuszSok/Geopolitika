package kaktusz.geopolitika.states;

import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class ClientStatesManager {

	private static Short2ObjectOpenHashMap<CommonStateInfo> stateInfos;
	private static short conflictStateId;

	@Nonnull
	public static CommonStateInfo getChunkOwner(int chunkX, int chunkZ, World world) {
		ChunksSavedData.ChunkInfo savedInfo = ChunksSavedData.get(world).getChunkInfo(new ChunkPos(chunkX, chunkZ));
		if(savedInfo == null || stateInfos == null)
			return CommonStateInfo.NONE;
		CommonStateInfo commonInfo = stateInfos.get(savedInfo.stateId);
		if(commonInfo == null)
			return CommonStateInfo.NONE;
		return commonInfo;
	}

	public static void setStateInfos(Short2ObjectOpenHashMap<CommonStateInfo> stateInfos, short conflictStateId) {
		ClientStatesManager.stateInfos = stateInfos;
		ClientStatesManager.conflictStateId = conflictStateId;
	}

	public static boolean isConflictState(CommonStateInfo state) {
		return state.id == conflictStateId;
	}
}
