package kaktusz.geopolitika.integration;

import com.feed_the_beast.ftblib.events.universe.UniverseLoadedEvent;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftbutilities.data.ClaimedChunk;
import com.feed_the_beast.ftbutilities.data.ClaimedChunks;
import com.feed_the_beast.ftbutilities.events.chunks.ChunkModifiedEvent;
import kaktusz.geopolitika.states.StatesManager;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalInt;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber
public class FTBUtilitiesIntegration {
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onUniversePreLoaded(UniverseLoadedEvent.Pre event)
	{
		ClaimedChunks.instance = new ClaimedChunksGeopolitikaOverride(event.getUniverse());
	}

	public static class ClaimedChunksGeopolitikaOverride extends ClaimedChunks {

		public ClaimedChunksGeopolitikaOverride(Universe u) {
			super(u);
		}

		@Override
		public boolean canPlayerModify(ForgePlayer player, ChunkDimPos pos, String perm) {
			return false; //all chunk modification is handled by the mod instead
		}

		@Override
		public void unclaimAllChunks(@Nullable ForgePlayer player, ForgeTeam team, OptionalInt dim) {
			for (ClaimedChunk chunk : getTeamChunks(team, dim))
			{
				ChunkDimPos pos = chunk.getPos();
				if(StatesManager.getChunkControlPointPos(pos.posX, pos.posZ, universe.world) != null) {
					continue; //chunk is claimed by a control point - disallow unclaiming it manually
				}

				if (chunk.isLoaded())
				{
					new ChunkModifiedEvent.Unloaded(chunk, player).post();
				}

				chunk.setLoaded(false);
				new ChunkModifiedEvent.Unclaimed(chunk, player).post();
				removeChunk(pos);
			}
			//NOTE: we handle unclaiming of chunks for teams being deleted in GameplayEventHandler
		}
	}
}
