package kaktusz.geopolitika.integration;

import com.google.common.collect.BiMap;
import io.netty.buffer.ByteBuf;
import kaktusz.geopolitika.Geopolitika;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PTEDisplay implements Comparable<PTEDisplay> {

	public static class RadiusHighlight {
		public final byte radius;
		public final int colour;
		public final short fillOpacity;

		/**
		 * @param radius Highlight radius in chunks (0-127)
		 */
		public RadiusHighlight(byte radius) {
			this(radius, MinimapIntegrationHelper.HIGHLIGHT_DEFAULT_COLOUR.rgba(), MinimapIntegrationHelper.HIGHLIGHT_DEFAULT_FILL_OPACITY);
		}
		public RadiusHighlight(byte radius, int colour, short fillOpacity) {
			this.radius = radius;
			this.colour = colour;
			this.fillOpacity = fillOpacity;
		}
	}

	public ItemStack displayStack;
	public double iconScale = 1.0d;
	public byte zOrder = 0;
	public int tint = 0x00000000;
	public final List<RadiusHighlight> radiusHighlights = new ArrayList<>();
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
		byte highlightsCount = buf.readByte();
		for (int i = 0; i < highlightsCount; i++) {
			radiusHighlights.add(new RadiusHighlight(buf.readByte(), buf.readInt(), buf.readUnsignedByte()));
		}
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
		buf.writeByte(radiusHighlights.size());
		for (RadiusHighlight highlight : radiusHighlights) {
			buf.writeByte(highlight.radius);
			buf.writeInt(highlight.colour);
			buf.writeByte(highlight.fillOpacity);
		}
		buf.writeFloat(labourContribution);
		buf.writeFloat(idealLabourContribution);

		int hoverIdx = stringsLUT.get(hoverText);
		buf.writeInt(hoverIdx);
	}

	/**
	 * @param radius Must not exceed 127
	 */
	public void addRadiusHighlight(int radius) {
		radiusHighlights.add(new RadiusHighlight((byte)radius));
	}

	/**
	 * @param radius Must not exceed 127
	 * @param fillOpacity Must not exceed 255
	 */
	public void addRadiusHighlight(int radius, int colour, short fillOpacity) {
		radiusHighlights.add(new RadiusHighlight((byte)radius, colour, fillOpacity));
	}

	public List<String> getLines() {
		if(lines == null) {
			lines = Arrays.asList(hoverText.replace("\t", "    ").split("\n"));
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
