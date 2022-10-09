package kaktusz.geopolitika.permaloaded.tileentities;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class PTEDisplay {

	public ItemStack displayStack;
	public byte zOrder = 0;
	public int tint = 0x00000000;
	public String hoverText;
	private List<String> lines = null;

	public PTEDisplay(ItemStack displayStack) {
		this.displayStack = displayStack;
	}

	public PTEDisplay(ByteBuf buf) {
		displayStack = ByteBufUtils.readItemStack(buf);
		zOrder = buf.readByte();
		tint = buf.readInt();
		short textLength = buf.readShort();
		hoverText = buf.readCharSequence(textLength, StandardCharsets.UTF_8).toString();
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeItemStack(buf, displayStack);
		buf.writeByte(zOrder);
		buf.writeInt(tint);
		if(hoverText.length() > Short.MAX_VALUE)
			hoverText = hoverText.substring(0, Short.MAX_VALUE);
		buf.writeShort(hoverText.length());
		buf.writeCharSequence(hoverText, StandardCharsets.UTF_8);
	}

	public List<String> getLines() {
		if(lines == null)
			lines = Arrays.asList(hoverText.replace("\t", "    ").split("\n").clone());

		return lines;
	}
}
