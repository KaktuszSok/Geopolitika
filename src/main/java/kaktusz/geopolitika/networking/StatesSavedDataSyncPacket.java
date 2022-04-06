package kaktusz.geopolitika.networking;

import io.netty.buffer.ByteBuf;
import kaktusz.geopolitika.states.ChunksSavedData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class StatesSavedDataSyncPacket implements IMessage {

	public ChunksSavedData data;

	@SuppressWarnings("unused") //required by forge
	public StatesSavedDataSyncPacket() {

	}

	public StatesSavedDataSyncPacket(ChunksSavedData data) {
		this.data = data;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		data = new ChunksSavedData();
		data.fromBytes(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		data.toBytes(buf);
	}

	public static class StatesSavedDataSyncHandler implements IMessageHandler<StatesSavedDataSyncPacket, IMessage> {

		@Override
		public IMessage onMessage(StatesSavedDataSyncPacket message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				ChunksSavedData.set(Minecraft.getMinecraft().world, message.data);
			});
			return null;
		}
	}
}
