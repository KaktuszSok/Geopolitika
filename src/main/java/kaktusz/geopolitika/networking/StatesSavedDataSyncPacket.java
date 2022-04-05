package kaktusz.geopolitika.networking;

import io.netty.buffer.ByteBuf;
import kaktusz.geopolitika.states.StatesSavedData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class StatesSavedDataSyncPacket implements IMessage {

	public StatesSavedData data;

	@SuppressWarnings("unused") //required by forge
	public StatesSavedDataSyncPacket() {

	}

	public StatesSavedDataSyncPacket(StatesSavedData data) {
		this.data = data;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		data = new StatesSavedData();
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
				StatesSavedData.set(Minecraft.getMinecraft().world, message.data);
			});
			return null;
		}
	}
}
