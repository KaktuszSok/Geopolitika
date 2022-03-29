package kaktusz.geopolitika.states;

import com.feed_the_beast.ftblib.lib.EnumTeamColor;
import com.feed_the_beast.ftblib.lib.EnumTeamStatus;
import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.TeamType;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftbutilities.FTBUtilities;
import com.feed_the_beast.ftbutilities.data.ClaimedChunk;
import com.feed_the_beast.ftbutilities.data.ClaimedChunks;
import com.feed_the_beast.ftbutilities.data.FTBUtilitiesTeamData;
import com.feed_the_beast.ftbutilities.events.chunks.ChunkModifiedEvent;
import kaktusz.geopolitika.Geopolitika;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Manages nation-states, which are based on FTBLib's forge teams.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StatesManager {

	private static StatesManager INSTANCE;
	public ForgeTeam CONFLICT_ZONE_TEAM;

	@SuppressWarnings("ConstantConditions")
	public StatesManager() {
		Universe universe = Universe.get();
		CONFLICT_ZONE_TEAM = universe.getTeam("conflict_zone");
		if(CONFLICT_ZONE_TEAM.type == TeamType.SERVER) { //conflict zone team was already created
			Geopolitika.logger.info("conflict zone team exists already");
			return;
		}
		else if(CONFLICT_ZONE_TEAM.isValid()) { //team exists with our reserved name - we can not stand for this.
			universe.removeTeam(CONFLICT_ZONE_TEAM);
		}

		CONFLICT_ZONE_TEAM = new ForgeTeam(universe, universe.generateTeamUID((short)1), "conflict_zone", TeamType.SERVER);
		CONFLICT_ZONE_TEAM.setColor(EnumTeamColor.RED);
		CONFLICT_ZONE_TEAM.setTitle("[Conflict Zone]");
		CONFLICT_ZONE_TEAM.setDesc("A conflict is ongoing in this area!");
		CONFLICT_ZONE_TEAM.setFreeToJoin(false);
		String anyoneAllowedString = EnumTeamStatus.NAME_MAP_PERMS.getName(EnumTeamStatus.ENEMY);
		ConfigGroup ftbutilsGroup = CONFLICT_ZONE_TEAM.getSettings().getGroup(FTBUtilities.MOD_ID);
		ftbutilsGroup.getValueInstance("explosions").getValue().setValueFromString(null, "true", false);
		ftbutilsGroup.getValueInstance("blocks_edit").getValue().setValueFromString(null, anyoneAllowedString, false);
		ftbutilsGroup.getValueInstance("blocks_interact").getValue().setValueFromString(null, anyoneAllowedString, false);
		ftbutilsGroup.getValueInstance("attack_entities").getValue().setValueFromString(null, anyoneAllowedString, false);
		ftbutilsGroup.getValueInstance("use_items").getValue().setValueFromString(null, anyoneAllowedString, false);
		Universe.get().addTeam(CONFLICT_ZONE_TEAM);

		Geopolitika.logger.info("created new conflict zone team with uid " + CONFLICT_ZONE_TEAM.getUID());
	}

	public static StatesManager get() {
		if(INSTANCE == null) {
			INSTANCE = new StatesManager();
		}

		return INSTANCE;
	}

	public static void restart() {
		Geopolitika.logger.info("StatesManager restarting...");
		INSTANCE = null;
	}

	private static ForgePlayer getForgePlayer(EntityPlayerMP player) {
		return Universe.get().getPlayer(player);
	}

	public static ForgeTeam getNoneState() {
		return Universe.get().getTeam((short)0);
	}

	public static ForgeTeam getStateFromUid(short uid) {
		return Universe.get().getTeam(uid);
	}

	/**
	 * Gets the state which this player belongs to.
	 */
	public static ForgeTeam getPlayerState(EntityPlayerMP player) {
		ForgeTeam team = getForgePlayer(player).team;
		if(team == null)
			return getNoneState();
		return team;
	}

	public static boolean isPlayerInState(EntityPlayerMP player, ForgeTeam state) {
		return state.equalsTeam(getForgePlayer(player).team);
	}

	/**
	 * Can this player modify the territorial claims of their state?
	 */
	public static boolean hasPlayerModifyClaimsAuthority(EntityPlayerMP player) {
		return getPlayerState(player).isModerator(getForgePlayer(player));
	}

	/**
	 * Gets the owner of this chunk. For conflict zones, the owner is a special server-owned state.
	 */
	public static ForgeTeam getChunkOwner(BlockPos blockPos, World world) {
		return getChunkOwner(blockPos.getX() >> 4, blockPos.getZ() >> 4, world);
	}

	/**
	 * Gets the owner of this chunk. For conflict zones, the owner is a special server-owned state.
	 */
	public static ForgeTeam getChunkOwner(int chunkX, int chunkZ, World world) {
		ForgeTeam team = ClaimedChunks.instance.getChunkTeam(new ChunkDimPos(chunkX, chunkZ, world.provider.getDimension()));
		if(team == null)
			return getNoneState();
		return team;
	}

	/**
	 * Claims the given chunk for the given state.
	 * If state is null, unclaims the chunk.
	 */
	public static void claimChunk(@Nullable ForgeTeam state, int cx, int cz, World world) {
		if(state == null) {
			ClaimedChunks.instance.unclaimChunk(null, new ChunkDimPos(cx, cz, world.provider.getDimension()));
			return;
		}

		FTBUtilitiesTeamData teamData = FTBUtilitiesTeamData.get(state);
		ClaimedChunk claimed = new ClaimedChunk(new ChunkDimPos(cx, cz, world.provider.getDimension()), teamData);
		ClaimedChunks.instance.addChunk(claimed);
		new ChunkModifiedEvent.Claimed(claimed, null).post();
	}

	/**
	 * Checks if there is an ongoing conflict in the given chunk.
	 */
	public static boolean isChunkInConflict(BlockPos pos, World world) {
		return getChunkOwner(pos, world).equalsTeam(get().CONFLICT_ZONE_TEAM);
	}

	/**
	 * Checks if there is an ongoing conflict in the given chunk.
	 */
	public static boolean isChunkInConflict(int cx, int cz, World world) {
		return getChunkOwner(cz, cz, world).equalsTeam(get().CONFLICT_ZONE_TEAM);
	}
}
