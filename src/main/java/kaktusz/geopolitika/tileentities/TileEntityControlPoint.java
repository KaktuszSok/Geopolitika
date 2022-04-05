package kaktusz.geopolitika.tileentities;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.util.misc.TimeType;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.handlers.GameplayEventHandler;
import kaktusz.geopolitika.init.ModConfig;
import kaktusz.geopolitika.states.StatesManager;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TileEntityControlPoint extends TileEntity implements ITickable {

	private static class OccupationProgress {
		private int warScore = 0;
		private final BossInfoServer bossBar;

		public OccupationProgress(ForgeTeam state) {
			ITextComponent title = new TextComponentString("")
					.appendSibling(state.getTitle())
					.appendText(" Occupation");
			bossBar = new BossInfoServer(title, BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
			bossBar.setDarkenSky(true);
		}

		public void setWarScore(int warScore) {
			this.warScore = warScore;
			bossBar.setPercent(Math.min(1.0f, warScore / 1000F));
		}
	}

	private static final String OWNER_NBT_TAG = Geopolitika.MODID + ":owner";
	private static final String CHUNKS_NBT_TAG = Geopolitika.MODID + ":chunks";
	private static final String REGION_NAME_NBT_TAG = Geopolitika.MODID + ":regionName";
	private static final String WAR_SCORES_NBT_TAG = Geopolitika.MODID + ":warScores";
	protected static final Style REGION_NAME_STYLE = new Style().setColor(TextFormatting.BLUE);
	public static final int OCCUPATION_DECAY_PER_SECOND = 3;
	public static final int OCCUPATION_GAIN_PER_SECOND = 2;

	private short ownerUid = 0;
	private ForgeTeam ownerCache;
	private NBTTagList claimedChunks = new NBTTagList();
	private String regionName = null;
	private final Map<Short, OccupationProgress> occupierWarScores = new HashMap<>();

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
			} else if(!StatesManager.getChunkOwner(pos, world).isValid()) { //fix chunks being unclaimed externally
				claimChunks(false);
			}
		});
	}

	@Override
	public void onChunkUnload() {
		onRemoveFromWorld();
		super.onChunkUnload();
	}

	@Override
	public void invalidate() {
		onRemoveFromWorld();
		super.invalidate();
	}

	private void onRemoveFromWorld() {
		GameplayEventHandler.loadedControlPoints.remove(this);
	}

	@Override
	public void update() {
		if(world.getWorldTime() % 20 != 0) //tick once per second
			return;

		if(occupierWarScores.isEmpty())
			return;

		STATES_LOOP:for (Short stateId : occupierWarScores.keySet()) {
			ForgeTeam state = StatesManager.getStateFromUid(stateId);
			for (EntityPlayerMP member : state.getOnlineMembers()) {
				if(member.world == world && member.dimension == world.provider.getDimension()
				&& pos.equals(StatesManager.getChunkControlPointPos(member.getPosition(), member.world))) { //member is in our region
					addWarScore(stateId, OCCUPATION_GAIN_PER_SECOND);
					continue STATES_LOOP;
				}
			}

			addWarScore(stateId, -OCCUPATION_DECAY_PER_SECOND);
		}
	}

	public void claimChunks(boolean conflict) {
		if(world.isRemote)
			return;

		unclaimChunks();
		if(!getOwner().isValid()) {
			return;
		}

		claimedChunks = new NBTTagList();
		final byte radius = ModConfig.controlPointClaimRadius;
		for (byte x = (byte)-radius; x <= radius; x++) {
			for (byte z = (byte)-radius; z <= radius; z++) {

				int cx = (pos.getX() >> 4) + x;
				int cz = (pos.getZ() >> 4) + z;
				if(conflict && StatesManager.getChunkControlPointPos(cx, cz, world) != null) {
					continue; //starting a conflict should not change regional borders
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

				NBTTagCompound chunkClaimTag = new NBTTagCompound();
				chunkClaimTag.setTag("x", new NBTTagByte(x));
				chunkClaimTag.setTag("z", new NBTTagByte(z));
				claimedChunks.appendTag(chunkClaimTag);

				StatesManager.claimChunk(this, conflict, cx, cz, world);
			}
		}
	}

	public void unclaimChunks() {
		for (NBTBase claimedChunk : claimedChunks) {
			byte xOffset = ((NBTTagCompound)claimedChunk).getByte("x");
			byte zOffset = ((NBTTagCompound)claimedChunk).getByte("z");
			int cx = (pos.getX() >> 4) + xOffset;
			int cz = (pos.getZ() >> 4) + zOffset;

			if(!pos.equals(StatesManager.getChunkControlPointPos(cx, cz, world))) {
				continue; //incorrect owner - something went weird
			}

			StatesManager.claimChunk(null, false, cx, cz, world);
		}
		claimedChunks = new NBTTagList();
	}

	public void unclaimChunk(int cx, int cz) {
		StatesManager.claimChunk(null, false, cx, cz, world);

		byte xOffset = (byte) (cx - (pos.getX() >> 4));
		byte zOffset = (byte) (cz - (pos.getZ() >> 4));
		for (int i = 0; i < claimedChunks.tagCount(); i++) {
			if(claimedChunks.getCompoundTagAt(i).getByte("x") == xOffset
			&& claimedChunks.getCompoundTagAt(i).getByte("z") == zOffset) {
				claimedChunks.removeTag(i);
				return;
			}
		}
	}

	public void beginConflict() {
		claimChunks(true);
	}

	public void endConflict() {
		clearWarScores();
		claimChunks(false);
	}

	public boolean isConflictOngoing() {
		return StatesManager.isChunkInConflict(pos, world);
	}

	public void beginOccupation(ForgeTeam occupiers) {
		if(!occupiers.isValid() || occupiers.equalsTeam(getOwner()) || occupierWarScores.containsKey(occupiers.getUID()))
			return;

		ITextComponent message = new TextComponentString("")
				.appendSibling(occupiers.getCommandTitle())
				.appendText(" has began an occupation of ")
				.appendSibling(getRegionName(true))
				.appendText("!");
		world.getMinecraftServer().getPlayerList().sendMessage(message);
		if(!isConflictOngoing()) {
			beginConflict();
		}
		addWarScore(occupiers, OCCUPATION_DECAY_PER_SECOND);
	}

	public void addWarScore(ForgeTeam state, int amount) {
		addWarScore(state.getUID(), amount);
	}


	public void addWarScore(short stateId, int amount) {
		OccupationProgress progress = occupierWarScores.computeIfAbsent(stateId, id -> new OccupationProgress(StatesManager.getStateFromUid(id)));
		int warScore = occupierWarScores.getOrDefault(stateId, ;
		warScore += amount;
		if(warScore >= 1000) {
			capitulate(stateId);
			return;
		}
		occupierWarScores.put(stateId, Math.max(warScore, 0));
		markDirty();
		if(warScore < 0 && checkIfOccupiersLost()) {
			repelOccupation();
		}
	}

	public int getWarScore(ForgeTeam state) {
		return occupierWarScores.getOrDefault(state.getUID(), 0);
	}

	private void clearWarScores() {
		occupierWarScores.clear();
		markDirty();
	}

	public void removeOccupier(short stateId) {
		occupierWarScores.remove(stateId);
		markDirty();
		if(occupierWarScores.isEmpty())
			endConflict();
	}

	protected boolean checkIfOccupiersLost() {
		for (Map.Entry<Short, Integer> entry : occupierWarScores.entrySet()) {
			if(entry.getValue() > 0) {
				return false;
			}
		}
		return true;
	}

	public void capitulate(short winningState) {
		ITextComponent message = new TextComponentString("")
				.appendSibling(getRegionName(true))
				.appendText(" has capitulated to ")
				.appendSibling(StatesManager.getStateFromUid(winningState).getCommandTitle());
		world.getMinecraftServer().getPlayerList().sendMessage(message);
		clearWarScores();
		setOwner(winningState);
		endConflict();
	}

	/**
	 * Called when the defending state wins against the occupiers
	 */
	protected void repelOccupation() {
		ITextComponent message = new TextComponentString("")
				.appendSibling(getRegionName(true))
				.appendSibling(new TextComponentString(" has resisted occupation by "));
		Short[] occupiers = occupierWarScores.keySet().toArray(new Short[0]);
		for (int i = 0; i < occupiers.length; i++) {
			if(i > 0) {
				if (i == occupiers.length - 1) {
					message.appendSibling(new TextComponentString(" and "));
				} else {
					message.appendSibling(new TextComponentString(", "));
				}
			}
			message.appendSibling(StatesManager.getStateFromUid(occupiers[i]).getCommandTitle());
		}
		world.getMinecraftServer().getPlayerList().sendMessage(message);

		endConflict();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setShort(OWNER_NBT_TAG, ownerUid);
		compound.setTag(CHUNKS_NBT_TAG, claimedChunks);
		compound.setString(REGION_NAME_NBT_TAG, regionName == null ? "" : regionName);
		NBTTagList warScoresTag = new NBTTagList();
		for (Short stateId : occupierWarScores.keySet()) {
			int warScore = occupierWarScores.get(stateId);
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
		claimedChunks = compound.getTagList(CHUNKS_NBT_TAG, 10);
		String regionName = compound.getString(REGION_NAME_NBT_TAG);
		if(regionName.isEmpty())
			regionName = null;
		this.regionName = regionName;

		if(compound.hasKey(WAR_SCORES_NBT_TAG)) {
			NBTTagList warScoresTag = compound.getTagList(WAR_SCORES_NBT_TAG, 10);
			for (int i = 0; i < warScoresTag.tagCount(); i++) {
				NBTTagCompound occupierTag = warScoresTag.getCompoundTagAt(i);
				short id = occupierTag.getShort("id");
				int score = occupierTag.getInteger("score");
				occupierWarScores.put(id, score);
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
