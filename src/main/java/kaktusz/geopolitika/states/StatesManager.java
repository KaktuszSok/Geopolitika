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
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandTitle;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
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
	 * Can this player modify this region's name?
	 */
	public static boolean hasPlayerRegionRenameAuthority(EntityPlayerMP player, TileEntityControlPoint regionControlPoint) {
		return regionControlPoint.getOwner().isModerator(getForgePlayer(player));
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
	 * Checks if the given control point position is closer to the chunk than its current control point.
	 * Note: Does not regard the states which control points belong to.
	 * If states are different, the chunk shouldn't be claimed by the new control point.
	 */
	public static boolean canControlPointClaimChunk(int cx, int cz, BlockPos controlPointPos, World world) {
		return StatesSavedData.get(world).canControlPointClaimChunk(new ChunkPos(cx, cz), controlPointPos);
	}

	@Nullable
	public static TileEntityControlPoint getChunkControlPoint(BlockPos pos, World world) {
		return StatesSavedData.get(world).getChunkControlPoint(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
	}

	@Nullable
	public static TileEntityControlPoint getChunkControlPoint(int cx, int cz, World world) {
		return StatesSavedData.get(world).getChunkControlPoint(new ChunkPos(cx, cz));
	}

	@Nullable
	public static BlockPos getChunkControlPointPos(int cx, int cz, World world) {
		return StatesSavedData.get(world).getChunkControlPointPos(new ChunkPos(cx, cz));
	}

	@Nullable
	public static BlockPos getChunkControlPointPos(BlockPos pos, World world) {
		return getChunkControlPointPos(pos.getX() >> 4, pos.getZ() >> 4, world);
	}

	public static ITextComponent getRegionName(BlockPos pos, World world, boolean includeStateInfo) {
		return getRegionName(pos.getX() >> 4, pos.getZ() >> 4, world, includeStateInfo);
	}

	public static ITextComponent getRegionName(int cx, int cz, World world, boolean includeStateInfo) {
		TileEntityControlPoint cp = getChunkControlPoint(cx, cz, world);
		if(cp == null)
			return new TextComponentString("Unclaimed");

		return cp.getRegionName(includeStateInfo);
	}

	/**
	 * Checks if the given chunk is either unclaimed or claimed by the given state.
	 */
	public static boolean canStateClaimChunk(ForgeTeam state, int cx, int cz, World world) {
		ForgeTeam owner = getChunkOwner(cx, cz, world);
		return !owner.isValid() || owner.equalsTeam(state);
	}

	/**
	 * Claims the given chunk for the given control point.
	 * If control point is null or has an invalid owner, unclaims the chunk.
	 */
	public static void claimChunk(@Nullable TileEntityControlPoint controlPoint, boolean conflict, int cx, int cz, World world) {
		if(controlPoint == null || !controlPoint.getOwner().isValid()) {
			ClaimedChunks.instance.unclaimChunk(null, new ChunkDimPos(cx, cz, world.provider.getDimension()));
			StatesSavedData.get(world).removeChunkControlPoint(new ChunkPos(cx, cz));
			return;
		}

		ForgeTeam owner = conflict ? get().CONFLICT_ZONE_TEAM : controlPoint.getOwner();
		FTBUtilitiesTeamData teamData = FTBUtilitiesTeamData.get(owner);
		ClaimedChunk claimed = new ClaimedChunk(new ChunkDimPos(cx, cz, world.provider.getDimension()), teamData);
		ClaimedChunks.instance.addChunk(claimed);
		new ChunkModifiedEvent.Claimed(claimed, null).post();

		StatesSavedData.get(world).setChunkControlPoint(new ChunkPos(cx, cz), controlPoint.getPos());
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

	/**
	 * Checks if the given state is the server-owned conflict team
	 */
	public static boolean isConflictTeam(ForgeTeam state) {
		return state.equalsTeam(get().CONFLICT_ZONE_TEAM);
	}
}
