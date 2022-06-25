package kaktusz.geopolitika.tileentities;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.util.misc.TimeType;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.handlers.GameplayEventHandler;
import kaktusz.geopolitika.handlers.OccupationEventHandler;
import kaktusz.geopolitika.init.ModConfig;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.states.StatesSavedData;
import kaktusz.geopolitika.util.MessageUtils;
import kaktusz.geopolitika.util.SoundUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandException;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.*;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TileEntityControlPoint extends TileEntity implements ITickable {

	public class OccupationProgress {
		private int warScore = 0;
		private final BossInfoServer bossBar;
		private final ForgeTeam occupiers;

		public OccupationProgress(ForgeTeam occupiers) {
			this.occupiers = occupiers;
			ITextComponent title = new TextComponentString("")
					.appendSibling(occupiers.getTitle())
					.appendText(" occupation of ")
					.appendSibling(getRegionName(true));
			bossBar = new BossInfoServer(title, BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
			bossBar.setDarkenSky(true);
		}

		public int getWarScore() {
			return warScore;
		}

		public void setWarScore(int warScore) {
			this.warScore = warScore;
		}

		public ForgeTeam getOccupiers() {
			return occupiers;
		}

		public void updateBossBar() {
			bossBar.setPercent(Math.min(1.0f, warScore / 1000F));
			ITextComponent title = new TextComponentString("")
					.appendSibling(occupiers.getTitle())
					.appendText(" occupation of ")
					.appendSibling(getRegionName(true));
			bossBar.setName(title);
		}
	}

	private static final String OWNER_NBT_TAG = Geopolitika.MODID + ":owner";
	private static final String REGION_NAME_NBT_TAG = Geopolitika.MODID + ":regionName";
	private static final String WAR_SCORES_NBT_TAG = Geopolitika.MODID + ":warScores";
	private static final String OCCUPATION_COOLDOWN_TAG = Geopolitika.MODID + ":cooldownEnd";
	protected static final Style REGION_NAME_STYLE = new Style().setColor(TextFormatting.BLUE);
	public static final int OCCUPATION_DECAY_PER_SECOND = ModConfig.warScoreDecay;
	public static final int OCCUPATION_GAIN_PER_SECOND = ModConfig.warScoreOccupationGain;
	private static final SoundEvent OCCUPATION_START_SOUND_ATTACKERS = SoundEvents.ENTITY_ENDERDRAGON_GROWL;
	private static final SoundEvent OCCUPATION_START_SOUND_DEFENDERS = SoundEvents.ENTITY_WITHER_AMBIENT;
	private static final SoundEvent OCCUPATION_WIN_SOUND = SoundEvents.ENTITY_WITHER_DEATH;
	private static final SoundEvent OCCUPATION_OTHER_WON_SOUND = SoundEvents.ENTITY_WITHER_HURT;
	private static final SoundEvent OCCUPATION_LOSE_SOUND = SoundEvents.ENTITY_WITHER_SPAWN;

	private short ownerUid = 0;
	private ForgeTeam ownerCache;
	private String regionName = null;
	private final Map<Short, OccupationProgress> occupierWarScores = new HashMap<>();
	/**
	 * Earliest time (as in System.currentTimeMillis()) when this control point can be the target of a new occupation.
	 */
	private long occupationCooldownEndTime = 0;
	private long highlightOccupiersCooldownEndTime = 0; //not persistent
	private long activeOccupationCooldownTicks = 0; //not persistent

	public ForgeTeam getOwner() {
		if(ownerCache == null)
			ownerCache = StatesManager.getStateFromUid(ownerUid);

		return ownerCache;
	}

	public void setOwner(@Nullable ForgeTeam owner) {
		this.ownerCache = owner;
		if(owner == null) {
			this.ownerUid = 0;
			unclaimChunks();
		} else {
			this.ownerUid = owner.getUID();
		}
		markDirty();
	}

	public void setOwner(short ownerUid) {
		this.ownerCache = null;
		this.ownerUid = ownerUid;
		markDirty();
	}

	@Override
	public void onLoad() {
		if(world.isRemote)
			return;

		GameplayEventHandler.loadedControlPoints.add(this);

		Universe.get().scheduleTask(TimeType.TICKS, 1, (u) -> {
			if(!getOwner().isValid()) { //owning state was deleted - unclaim the territory
				unclaimChunks();
			} else if(!StatesManager.getChunkOwner(pos, world).isValid()
					|| StatesManager.getChunkControlPointPos(pos, world) == null) { //fix chunks being unclaimed externally
				claimChunks(false);
			}
		});
	}

	@Override
	public void onChunkUnload() {
		onRemoveFromLoadedWorld();
		super.onChunkUnload();
	}

	@Override
	public void invalidate() {
		onRemoveFromLoadedWorld();
		super.invalidate();
	}

	private void onRemoveFromLoadedWorld() {
		GameplayEventHandler.loadedControlPoints.remove(this);
	}

	public void onRemoveFromWorld() {
		clearWarScores();
		unclaimChunks();
	}

	@Override
	public void update() {
		if(world.isRemote || world.getWorldTime() % 20 != 0) //tick once per second
			return;

		if(occupierWarScores.isEmpty())
			return;

		if(activeOccupationCooldownTicks > 0)
			activeOccupationCooldownTicks -= 20;

		//inefficient but fuck it
		//noinspection ConstantConditions
		for (EntityPlayerMP player : world.getMinecraftServer().getPlayerList().getPlayers()) {
			if(isEntityInRegion(player, false)) {
				for (OccupationProgress progress : occupierWarScores.values()) {
					progress.bossBar.addPlayer(player);
				}
			} else {
				for (OccupationProgress progress : occupierWarScores.values()) {
					progress.bossBar.removePlayer(player);
				}
			}
		}

		//check if region is actively defended
		boolean activelyDefended = false;
		for (EntityPlayerMP member : getOwner().getOnlineMembers()) {
			if(isEntityInRegion(member)) { //at least one defender is in our region - war score should not be gained by occupiers
				activelyDefended = true;
				break;
			}
		}

		//check if region is actively occupied
		STATES_LOOP:for (Short stateId : occupierWarScores.keySet()) {
			ForgeTeam state = StatesManager.getStateFromUid(stateId);
			for (EntityPlayerMP member : state.getOnlineMembers()) {
				if(isEntityInRegion(member)) { //at least one member is in our region - the state's war score shall not decay
					if(!activelyDefended) { //no defenders in region - the state shall gain war score
						addWarScore(stateId, OCCUPATION_GAIN_PER_SECOND);
						if (occupierWarScores.isEmpty())
							return;
					}
					continue STATES_LOOP;
				}
			}

			addWarScore(stateId, -OCCUPATION_DECAY_PER_SECOND); //no members in region - the state shall lose war score
			if(occupierWarScores.isEmpty())
				return;
		}

		for (OccupationProgress progress : occupierWarScores.values()) {
			progress.updateBossBar();
		}
	}

	public boolean isEntityInRegion(EntityLivingBase entity) {
		return isEntityInRegion(entity, true);
	}
	public boolean isEntityInRegion(EntityLivingBase entity, boolean mustBeAlive) {
		return (!mustBeAlive || entity.isEntityAlive())
				&& entity.world == world
				&& entity.dimension == world.provider.getDimension()
				&& pos.equals(StatesManager.getChunkControlPointPos(entity.getPosition(), entity.getEntityWorld()));
	}

	public void claimChunks(boolean conflict) {
		if(world.isRemote)
			return;

		unclaimChunks();
		if(!getOwner().isValid()) {
			return;
		}

		final byte radius = ModConfig.controlPointClaimRadius;
		for (byte x = (byte)-radius; x <= radius; x++) {
			for (byte z = (byte)-radius; z <= radius; z++) {

				int cx = (pos.getX() >> 4) + x;
				int cz = (pos.getZ() >> 4) + z;
				if(conflict && StatesManager.getChunkControlPointPos(cx, cz, world) != null) {
					continue; //starting a conflict should not change other region's borders
				}
				if(!StatesManager.canStateClaimChunk(getOwner(), cx, cz, world)) {
					continue; //chunk is claimed by a different state
				}
				if(!StatesManager.canControlPointClaimChunk(cx, cz, getPos(), world)) {
					continue; //chunk already claimed and its control point is closer to it than this one is
				}

				TileEntityControlPoint currentControlPoint = StatesManager.getChunkControlPoint(cx, cz, world);
				if(currentControlPoint != null) {
					currentControlPoint.unclaimChunk(cx, cz);
				}

				StatesManager.claimChunk(this, conflict, cx, cz, world);
			}
		}
	}

	public void unclaimChunks() {
		final byte radius = ModConfig.controlPointClaimRadius;
			for (byte x = (byte)-radius; x <= radius; x++) {
				for (byte z = (byte)-radius; z <= radius; z++) {
				int cx = (pos.getX() >> 4) + x;
				int cz = (pos.getZ() >> 4) + z;

				if(!pos.equals(StatesManager.getChunkControlPointPos(cx, cz, world))) {
					continue; //we are not the owner - dont unclaim
				}

				StatesManager.claimChunk(null, false, cx, cz, world);
			}
		}
	}

	public void unclaimChunk(int cx, int cz) {
		StatesManager.claimChunk(null, false, cx, cz, world);
	}

	public void beginConflict() {
		claimChunks(true);
		highlightOccupiersCooldownEndTime = System.currentTimeMillis() + ModConfig.highlightOccupiersCooldown;
	}

	public void endConflict() {
		clearWarScores();
		claimChunks(false);
	}

	public boolean isConflictOngoing() {
		return StatesManager.isChunkInConflict(pos, world);
	}

	/**
	 * Tries to begin an occupation, throwing an exception if prerequisites are not met.
	 * An exception will not be thrown if prerequisites are met but beginOccupation(..) fails,
	 * i.e. if occupiers are already occupying this state or if the occupiers are not valid.
	 *
	 * @param bypassBalanceMeasures Should the measures to improve gameplay fairness be bypassed? (cooldown, allow attacking offline states, etc)
	 */
	public void tryBeginOccupation(ForgeTeam occupiers, boolean bypassBalanceMeasures) throws CommandException {
		if(occupiers.equalsTeam(getOwner())) { //same state
			throw new CommandException(MessageUtils.getCommandErrorKey("friendly_territory"));
		}

		if(!bypassBalanceMeasures) {
			long cooldownTimeLeft = StatesSavedData.get(world).getOccupationCooldown(occupiers.getUID(), getOwner().getUID());
			if(cooldownTimeLeft > 0) { //state in cooldown
				throw new CommandException(MessageUtils.getCommandErrorKey("state_in_cooldown"),
						getOwner().getCommandTitle(),
						DurationFormatUtils.formatDurationHMS(cooldownTimeLeft));
			}

			cooldownTimeLeft = getOccupyCooldownTimeLeft();
			if (cooldownTimeLeft > 0) { //region in cooldown
				throw new CommandException(MessageUtils.getCommandErrorKey("region_in_cooldown"), DurationFormatUtils.formatDurationHMS(cooldownTimeLeft));
			}

			if (getOwner().getOnlineMembers().isEmpty() && !getOwner().getMembers().isEmpty()) { //no defenders online
				throw new CommandException(MessageUtils.getCommandErrorKey("cant_occupy_offline_state"), getOwner().getCommandTitle());
			}
		}

		beginOccupation(occupiers);
	}

	/**
	 * Force-begins an occupation (does not check prerequisites)
	 */
	public void beginOccupation(ForgeTeam occupiers) {
		if(occupiers.equalsTeam(getOwner()) //same state
				|| !occupiers.isValid() //invalid state
				|| occupierWarScores.containsKey(occupiers.getUID())) //already occuping
			return;

		ITextComponent message = new TextComponentString("")
				.appendSibling(occupiers.getCommandTitle())
				.appendText(" has began an occupation of ")
				.appendSibling(getRegionName(true))
				.appendText("!");
		//noinspection ConstantConditions
		MessageUtils.broadcastImportantMessage(world.getMinecraftServer(), message);
		SoundUtils.playSoundForState(occupiers, OCCUPATION_START_SOUND_ATTACKERS, 1f, 1f);
		SoundUtils.playSoundForState(getOwner(), OCCUPATION_START_SOUND_DEFENDERS, 1f, 1f);
		if(!isConflictOngoing()) {
			beginConflict();
		}
		occupierWarScores.put(occupiers.getUID(), new OccupationProgress(occupiers));
		addWarScore(occupiers, ModConfig.warScoreStartingAmount);
	}

	public void addWarScore(ForgeTeam state, int amount) {
		addWarScore(state.getUID(), amount);
	}


	public void addWarScore(short stateId, int amount) {
		OccupationProgress progress = occupierWarScores.get(stateId);
		if(progress == null)
			return;

		int oldWarScore = progress.warScore;
		int warScore = oldWarScore + amount;
		if(warScore >= 1000) {
			capitulate(stateId);
			return;
		} else {
			int[] thresholds = {900, 750, 500, 300};
			for (int threshold : thresholds) {
				if (warScore >= threshold && oldWarScore < threshold) {
					alertOccupationProgress(StatesManager.getStateFromUid(stateId), threshold);
					break;
				}
			}
		}
		progress.setWarScore(Math.max(warScore, 0));
		markDirty();
		if(warScore < 0 && checkIfOccupiersLost()) {
			resistOccupation();
		}
	}

	public int getWarScore(ForgeTeam state) {
		return getWarScore(state.getUID());
	}

	public int getWarScore(short stateId) {
		OccupationProgress progress = occupierWarScores.get(stateId);
		if(progress == null)
			return 0;
		return progress.warScore;
	}

	private void clearWarScores() {
		for (OccupationProgress progress : occupierWarScores.values()) {
			if(progress != null) {
				//clear boss bar for all players who see it
				for (EntityPlayerMP playerMP : progress.bossBar.getPlayers().toArray(new EntityPlayerMP[0])) {
					progress.bossBar.removePlayer(playerMP);
				}
			}
		}
		occupierWarScores.clear();
		markDirty();
	}

	public boolean isBeingOccupiedBy(ForgeTeam state) {
		return occupierWarScores.containsKey(state.getUID());
	}

	/**
	 * Gets the occupier with the most war score, if any
	 */
	@Nullable
	public OccupationProgress getWinningOccupier() {
		if(occupierWarScores.isEmpty())
			return null;
		return occupierWarScores.values().stream().max(Comparator.comparingInt(o -> o.warScore)).get();
	}

	public void removeOccupier(short stateId) {
		OccupationProgress removed = occupierWarScores.remove(stateId);
		if(removed != null) {
			//clear boss bar for all players who see it
			for (EntityPlayerMP playerMP : removed.bossBar.getPlayers().toArray(new EntityPlayerMP[0])) {
				removed.bossBar.removePlayer(playerMP);
			}
		}
		markDirty();
		if(occupierWarScores.isEmpty())
			endConflict();
	}

	protected boolean checkIfOccupiersLost() {
		for (Map.Entry<Short, OccupationProgress> entry : occupierWarScores.entrySet()) {
			if(entry.getValue().warScore > 0) {
				return false;
			}
		}
		return true;
	}

	public void capitulate(short winningStateId) {
		ForgeTeam winningState = StatesManager.getStateFromUid(winningStateId);
		ITextComponent message = new TextComponentString("")
				.appendSibling(getRegionName(true))
				.appendText(" has capitulated to ")
				.appendSibling(winningState.getCommandTitle());
		SoundUtils.playSoundForState(winningState, OCCUPATION_WIN_SOUND, 1f, 1.5f);
		SoundUtils.playSoundForState(getOwner(), OCCUPATION_LOSE_SOUND, 1f, 1f);
		for (short occupierId : occupierWarScores.keySet()) {
			if(occupierId == winningStateId)
				continue;

			SoundUtils.playSoundForState(StatesManager.getStateFromUid(occupierId), OCCUPATION_OTHER_WON_SOUND, 1f, 0.65f);
		}
		//noinspection ConstantConditions
		MessageUtils.broadcastImportantMessage(world.getMinecraftServer(), message);
		clearWarScores();
		setOwner(winningStateId);
		endConflict();
		setOccupationCooldown(ModConfig.occupationCooldownOnCapitulate);
	}

	/**
	 * Called when the defending state wins against the occupiers
	 */
	protected void resistOccupation() {
		ITextComponent message = new TextComponentString("")
				.appendSibling(getRegionName(true))
				.appendSibling(new TextComponentString(" has resisted occupation by "));
		SoundUtils.playSoundForState(getOwner(), OCCUPATION_WIN_SOUND, 1f, 1.5f);
		Short[] occupiers = occupierWarScores.keySet().toArray(new Short[0]);
		for (int i = 0; i < occupiers.length; i++) {
			ForgeTeam state = StatesManager.getStateFromUid(occupiers[i]);
			StatesSavedData.get(world).setOccupationCooldown(occupiers[i], getOwner().getUID(), ModConfig.occupationCooldownOnDefend);
			SoundUtils.playSoundForState(state, OCCUPATION_LOSE_SOUND, 1f, 1f);

			if (i > 0) {
				if (i == occupiers.length - 1) {
					message.appendSibling(new TextComponentString(" and "));
				} else {
					message.appendSibling(new TextComponentString(", "));
				}
			}
			message.appendSibling(StatesManager.getStateFromUid(occupiers[i]).getCommandTitle());
		}
		//noinspection ConstantConditions
		MessageUtils.broadcastImportantMessage(world.getMinecraftServer(), message);

		endConflict();
	}

	public void setOccupationCooldown(int cooldownMinutes) {
		occupationCooldownEndTime = System.currentTimeMillis() + (long) cooldownMinutes*60*1000;
	}

	public long getOccupyCooldownTimeLeft() {
		return occupationCooldownEndTime - System.currentTimeMillis();
	}

	public void tryHighlightOccupiers(EntityPlayerMP user) {
		long cooldownLeft = highlightOccupiersCooldownEndTime - System.currentTimeMillis();
		if(cooldownLeft > 0) {
			MessageUtils.sendErrorMessage(user, "cp_highlight_cooldown", DurationFormatUtils.formatDurationHMS(cooldownLeft));
			return;
		}

		int occupiersAmount = 0;
		if(isConflictOngoing()) {
			for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, (p ->
				p != null && isBeingOccupiedBy(StatesManager.getPlayerState(p)) && isEntityInRegion(p)
			))) {
				player.addPotionEffect(new PotionEffect(MobEffects.GLOWING, ModConfig.highlightOccupiersDuration*20, 0, true, false));
				occupiersAmount++;
			}
		}
		if(occupiersAmount > 0) {
			highlightOccupiersCooldownEndTime = System.currentTimeMillis() + ModConfig.highlightOccupiersCooldown*1000L;
			MessageUtils.sendInfoMessage(user, new TextComponentTranslation("geopolitika.info.cp_highlight_success"));
		} else {
			MessageUtils.sendInfoMessage(user, new TextComponentTranslation("geopolitika.info.cp_highlight_nobody"));
		}
	}

	public void tryActivelyOccupy(EntityPlayerMP attacker) {
		if(activeOccupationCooldownTicks > 0)
			return;

		ForgeTeam state = StatesManager.getPlayerState(attacker);
		if(!isBeingOccupiedBy(state))
			return;

		activeOccupationCooldownTicks = 20;
		addWarScore(state, ModConfig.warScoreActiveOccupationGain);
		MessageUtils.displayActionbar(attacker,
				new TextComponentTranslation("geopolitika.info.gain_war_score_rightclick", ModConfig.warScoreActiveOccupationGain)
				.setStyle(OccupationEventHandler.GAIN_WAR_SCORE_MESSAGE_STYLE));
	}

	public void alertOccupationProgress(ForgeTeam occupiers, int warScore) {
		ITextComponent message = new TextComponentTranslation(
				"geopolitika.info.occupation_progress",
				getRegionName(true),
				new TextComponentString(warScore/10 + "%").setStyle(MessageUtils.BOLD_STYLE),
				occupiers.getCommandTitle());
		MessageUtils.sendMessageToState(getOwner(), message);
		MessageUtils.sendMessageToState(occupiers, message);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setShort(OWNER_NBT_TAG, ownerUid);
		compound.setString(REGION_NAME_NBT_TAG, regionName == null ? "" : regionName);
		compound.setLong(OCCUPATION_COOLDOWN_TAG, occupationCooldownEndTime);
		NBTTagList warScoresTag = new NBTTagList();
		for (Short stateId : occupierWarScores.keySet()) {
			int warScore = occupierWarScores.get(stateId).warScore;
			if(warScore == 0)
				continue;

			NBTTagCompound occupierTag = new NBTTagCompound();
			occupierTag.setShort("id", stateId);
			occupierTag.setInteger("score", warScore);
			warScoresTag.appendTag(occupierTag);
		}
		if(!warScoresTag.isEmpty()) {
			compound.setTag(WAR_SCORES_NBT_TAG, warScoresTag);
		}
		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		setOwner(compound.getShort(OWNER_NBT_TAG));
		String regionName = compound.getString(REGION_NAME_NBT_TAG);
		if(regionName.isEmpty())
			regionName = null;
		this.regionName = regionName;
		occupationCooldownEndTime = compound.getLong(OCCUPATION_COOLDOWN_TAG);

		if(compound.hasKey(WAR_SCORES_NBT_TAG)) {
			NBTTagList warScoresTag = compound.getTagList(WAR_SCORES_NBT_TAG, 10);
			for (int i = 0; i < warScoresTag.tagCount(); i++) {
				NBTTagCompound occupierTag = warScoresTag.getCompoundTagAt(i);
				short id = occupierTag.getShort("id");
				int score = occupierTag.getInteger("score");
				OccupationProgress progress = new OccupationProgress(StatesManager.getStateFromUid(id));
				progress.setWarScore(score);
				occupierWarScores.put(id, progress);
			}
		}

		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound tag = super.getUpdateTag();
		tag.setShort(OWNER_NBT_TAG, ownerUid);
		return tag;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		setOwner(tag.getShort(OWNER_NBT_TAG));
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, -1, this.getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		super.onDataPacket(net, pkt);
		handleUpdateTag(pkt.getNbtCompound());
	}

	public boolean setRegionName(@Nullable String regionName) {
		if(regionName != null && (regionName.length() > 64 || regionName.isEmpty()))
			return false; //too long or empty

		this.regionName = regionName;
		markDirty();
		return true;
	}

	public ITextComponent getRegionName(boolean includeStateInfo) {
		String raw;
		if(regionName == null)
			raw = "Unnamed Region (" + pos.getX() + "," + pos.getZ() + ")";
		else
			raw = regionName;

		if(!includeStateInfo)
			return new TextComponentString(raw).setStyle(REGION_NAME_STYLE);
		else
			return new TextComponentString(raw).setStyle(getOwner().getCommandTitle().getStyle());
	}
}
