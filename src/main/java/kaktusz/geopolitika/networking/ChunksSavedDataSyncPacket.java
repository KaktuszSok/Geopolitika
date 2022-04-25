package kaktusz.geopolitika.networking;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.states.ChunksSavedData;
import kaktusz.geopolitika.states.ClientStatesManager;
import kaktusz.geopolitika.states.CommonStateInfo;
import kaktusz.geopolitika.states.StatesManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChunksSavedDataSyncPacket implements IMessage {

	public ChunksSavedData data;
	public final Short2ObjectOpenHashMap<CommonStateInfo> stateInfos = new Short2ObjectOpenHashMap<>();
	public short conflictStateId;

	@SuppressWarnings("unused") //required by forge
	public ChunksSavedDataSyncPacket() {

	}

	public ChunksSavedDataSyncPacket(ChunksSavedData data) {
		this.data = data;
		for (ForgeTeam state : Universe.get().getTeams()) {
			stateInfos.put(state.getUID(), new CommonStateInfo(state));
		}
		conflictStateId = StatesManager.get().CONFLICT_ZONE_TEAM.getUID();
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		data = new ChunksSavedData();
		data.fromBytes(buf);
		short statesCount = buf.readShort();
		for (short i = 0; i < statesCount; i++) {
			short id = buf.readShort();
			short colourIdx = buf.readShort();
			stateInfos.put(id, new CommonStateInfo(id, colourIdx));
		}
		conflictStateId = buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		data.toBytes(buf);
		buf.writeShort(stateInfos.size());
		stateInfos.forEach(
				(id, info) -> {
					buf.writeShort(id);
					buf.writeShort(info.colour.ordinal());
				}
		);
		buf.writeShort(conflictStateId);
	}

	public static class ChunksSavedDataSyncHandler implements IMessageHandler<ChunksSavedDataSyncPacket, IMessage> {

		@Override
		public IMessage onMessage(ChunksSavedDataSyncPacket message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				ChunksSavedData.set(Geopolitika.PROXY.getClientWorld(), message.data);
				ClientStatesManager.setStateInfos(message.stateInfos, message.conflictStateId);
			});
			return null;
		}
	}
}
