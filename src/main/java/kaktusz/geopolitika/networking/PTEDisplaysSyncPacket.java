package kaktusz.geopolitika.networking;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.buffer.ByteBuf;
import kaktusz.geopolitika.integration.MinimapIntegrationHelper;
import kaktusz.geopolitika.permaloaded.PermaloadedSavedData;
import kaktusz.geopolitika.permaloaded.tileentities.DisplayablePTE;
import kaktusz.geopolitika.integration.PTEDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PTEDisplaysSyncPacket implements IMessage {

	public static final int VIEW_DISTANCE = 16;

	private final Map<BlockPos, PTEDisplay> data = new HashMap<>();

	@SuppressWarnings("unused") //required by forge
	public PTEDisplaysSyncPacket() {

	}

	public PTEDisplaysSyncPacket(BlockPos viewCentre, World world) {
		ChunkPos chunkPos = new ChunkPos(viewCentre);
		data.clear();
		for (DisplayablePTE displayablePTE : PermaloadedSavedData.get(world)
				.findTileEntitiesByInterface(DisplayablePTE.class, chunkPos, VIEW_DISTANCE))
		{
			PTEDisplay display = displayablePTE.getDisplay();
			if(display == null)
				continue;
			data.put(displayablePTE.getPermaTileEntity().getPosition(), display);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		data.clear();

		//read LUTs
		List<ItemStack> stacksLUT = new ArrayList<>();
		BiMap<String, Integer> stringsLUT = HashBiMap.create();
		int stacksCount = buf.readInt();
		for (int i = 0; i < stacksCount; i++) {
			ItemStack stack = ByteBufUtils.readItemStack(buf);
			stacksLUT.add(stack);
		}
		int stringsCount = buf.readInt();
		for (int i = 0; i < stringsCount; i++) {
			int idx = buf.readInt();
			String str = ByteBufUtils.readUTF8String(buf);
			stringsLUT.put(str, idx);
		}

		//read displays
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			BlockPos pos = new BlockPos(buf.readInt(), buf.readShort(), buf.readInt());
			PTEDisplay display = new PTEDisplay(buf, stringsLUT, stacksLUT);
			data.put(pos, display);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		int start = buf.writerIndex();

		//generate LUTs
		List<ItemStack> stacksLUT = new ArrayList<>();
		BiMap<String, Integer> stringsLUT = HashBiMap.create();
		for (Map.Entry<BlockPos, PTEDisplay> kvp : data.entrySet()) {
			//populate stack LUT:
			Integer stackIdx = null;
			for (int i = 0; i < stacksLUT.size(); i++) {
				if(ItemStack.areItemStacksEqual(kvp.getValue().displayStack, stacksLUT.get(i))) {
					stackIdx = i;
					break;
				}
			}
			if(stackIdx == null) {
				stacksLUT.add(kvp.getValue().displayStack);
			}

			//populate string LUT:
			Integer hoverIdx = stringsLUT.get(kvp.getValue().hoverText);
			if(hoverIdx == null) {
				hoverIdx = stringsLUT.size();
				stringsLUT.put(kvp.getValue().hoverText, hoverIdx);
			}
		}

		//write LUTs
		buf.writeInt(stacksLUT.size());
		for (ItemStack itemStack : stacksLUT) {
			ByteBufUtils.writeItemStack(buf, itemStack);
		}
		buf.writeInt(stringsLUT.size());
		for (Map.Entry<String, Integer> kvp : stringsLUT.entrySet()) {
			buf.writeInt(kvp.getValue());
			ByteBufUtils.writeUTF8String(buf, kvp.getKey());
		}

		//write displays
		buf.writeInt(data.size());
		for (Map.Entry<BlockPos, PTEDisplay> kvp : data.entrySet()) {
			BlockPos pos = kvp.getKey();
			buf.writeInt(pos.getX()).writeShort(pos.getY()).writeInt(pos.getZ());
			kvp.getValue().toBytes(buf, stringsLUT, stacksLUT);
		}
	}

	public static class PTEDisplaysSyncHandler implements IMessageHandler<PTEDisplaysSyncPacket, IMessage> {

		@Override
		public IMessage onMessage(PTEDisplaysSyncPacket message, MessageContext ctx) {
			MinimapIntegrationHelper.updatePTEDisplays(message.data, VIEW_DISTANCE);
			return null;
		}
	}
}
