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
	public float labourContribution = 0;
	public float idealLabourContribution = 0;
	public String hoverText;
	private List<String> lines = null;
	private boolean cached = false; //becomes true once a new batch of displays comes in and these are no longer necessarily up-to-date

	public PTEDisplay(ItemStack displayStack) {
		this.displayStack = displayStack;
	}

	public PTEDisplay(ByteBuf buf) {
		displayStack = ByteBufUtils.readItemStack(buf);
		zOrder = buf.readByte();
		tint = buf.readInt();
		labourContribution = buf.readFloat();
		idealLabourContribution = buf.readFloat();
		short textLength = buf.readShort();
		hoverText = buf.readCharSequence(textLength, StandardCharsets.UTF_8).toString();
	}

	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeItemStack(buf, displayStack);
		buf.writeByte(zOrder);
		buf.writeInt(tint);
		buf.writeFloat(labourContribution);
		buf.writeFloat(idealLabourContribution);
		if(hoverText.length() > Short.MAX_VALUE)
			hoverText = hoverText.substring(0, Short.MAX_VALUE);
		buf.writeShort(hoverText.length());
		buf.writeCharSequence(hoverText, StandardCharsets.UTF_8);
	}

	public List<String> getLines() {
		if(lines == null) {
			lines = Arrays.asList(hoverText.replace("\t", "    ").split("\n").clone());
		}

		return lines;
	}

	public void setCached() {
		cached = true;
	}

	public boolean isCached() {
		return cached;
	}
}
