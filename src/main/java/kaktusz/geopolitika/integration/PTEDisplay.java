package kaktusz.geopolitika.integration;

import com.google.common.collect.BiMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public class PTEDisplay implements Comparable<PTEDisplay> {

	public ItemStack displayStack;
	public double iconScale = 1.0d;
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

	public PTEDisplay(ByteBuf buf, BiMap<String, Integer> stringsLUT, List<ItemStack> stacksLUT) {
		int stackIdx = buf.readInt();
		displayStack = stacksLUT.get(stackIdx);
		iconScale = buf.readDouble();
		zOrder = buf.readByte();
		tint = buf.readInt();
		labourContribution = buf.readFloat();
		idealLabourContribution = buf.readFloat();
		int hoverIdx = buf.readInt();
		hoverText = stringsLUT.inverse().get(hoverIdx);
	}

	public void toBytes(ByteBuf buf, BiMap<String, Integer> stringsLUT, List<ItemStack> stacksLUT) {
		int stackIdx = -1;
		for (int i = 0; i < stacksLUT.size(); i++) {
			if(ItemStack.areItemStacksEqual(displayStack, stacksLUT.get(i))) {
				stackIdx = i;
				break;
			}
		}
		buf.writeInt(stackIdx);

		buf.writeDouble(iconScale);
		buf.writeByte(zOrder);
		buf.writeInt(tint);
		buf.writeFloat(labourContribution);
		buf.writeFloat(idealLabourContribution);

		int hoverIdx = stringsLUT.get(hoverText);
		buf.writeInt(hoverIdx);
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

	@Override
	public int compareTo(PTEDisplay o) {
		return zOrder - o.zOrder;
	}
}
