package kaktusz.geopolitika.tileentities;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.init.ModConfig;
import kaktusz.geopolitika.states.StatesManager;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TileEntityControlPoint extends TileEntity {

	private static final String OWNER_NBT_TAG = Geopolitika.MODID + ":owner";
	private static final String CHUNKS_NBT_TAG = Geopolitika.MODID + ":chunks";

	private short ownerUid = 0;
	private ForgeTeam ownerCache;
	private NBTTagList claimedChunks = new NBTTagList();

	public ForgeTeam getOwner() {
		if(ownerCache == null)
			ownerCache = StatesManager.getStateFromUid(ownerUid);

		return ownerCache;
	}

	public void setOwner(@Nullable ForgeTeam owner) {
		this.ownerCache = owner;
		if(owner == null)
			this.ownerUid = 0;
		else
			this.ownerUid = owner.getUID();
	}

	public void setOwner(short ownerUid) {
		this.ownerCache = null;
		this.ownerUid = ownerUid;
	}

	@Override
	public void onLoad() {
		super.onLoad();
	}

	public void claimChunks(ForgeTeam claimingTeam) {
		if(world.isRemote)
			return;

		unclaimChunks();
		if(!claimingTeam.isValid()) {
			return;
		}

		claimedChunks = new NBTTagList();
		final byte radius = ModConfig.controlPointClaimRadius;
		for (byte x = (byte)-radius; x <= radius; x++) {
			for (byte z = (byte)-radius; z <= radius; z++) {

				int cx = (pos.getX() >> 4) + x;
				int cz = (pos.getZ() >> 4) + z;
				if(StatesManager.getChunkOwner(cx, cz, world).isValid()) {
					continue; //chunk already claimed
				}

				NBTTagCompound chunkClaimTag = new NBTTagCompound();
				chunkClaimTag.setTag("x", new NBTTagByte(x));
				chunkClaimTag.setTag("z", new NBTTagByte(z));
				claimedChunks.appendTag(chunkClaimTag);

				StatesManager.claimChunk(claimingTeam, cx, cz, world);
			}
		}
	}

	public void unclaimChunks() {
		for (NBTBase claimedChunk : claimedChunks) {
			byte xOffset = ((NBTTagCompound)claimedChunk).getByte("x");
			byte zOffset = ((NBTTagCompound)claimedChunk).getByte("z");
			StatesManager.claimChunk(null, (pos.getX() >> 4) + xOffset, (pos.getZ() >> 4) + zOffset, world);
		}
	}

	public void beginConflict() {
		claimChunks(StatesManager.get().CONFLICT_ZONE_TEAM);
	}

	public void endConflict() {
		claimChunks(getOwner());
	}

	public boolean isConflictOngoing() {
		return StatesManager.isChunkInConflict(pos, world);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setShort(OWNER_NBT_TAG, ownerUid);
		compound.setTag(CHUNKS_NBT_TAG, claimedChunks);
		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		setOwner(compound.getShort(OWNER_NBT_TAG));
		claimedChunks = compound.getTagList(CHUNKS_NBT_TAG, 10);
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
}
