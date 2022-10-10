package kaktusz.geopolitika.integration;

import com.feed_the_beast.ftblib.lib.icon.Color4I;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.init.ModConfig;
import kaktusz.geopolitika.networking.PTEDisplaysSyncPacket;
import kaktusz.geopolitika.permaloaded.tileentities.PTEDisplay;
import kaktusz.geopolitika.states.ClientStatesManager;
import kaktusz.geopolitika.states.CommonStateInfo;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.ColourUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@SideOnly(Side.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinimapIntegrationHelper {

	private static final Color4I CONFLICT_COLOUR = Color4I.rgba(255, 0, 0, 50);
	private static final Map<BlockPos, PTEDisplay> PTE_DISPLAYS = new ConcurrentHashMap<>();
	private static final Map<BlockPos, PTEDisplay> PTE_DISPLAYS_CACHED = new ConcurrentHashMap<>();
	private static int MAX_CACHE_SIZE = 10000;
	private static final ITextComponent CACHED_DISPLAY_TEXTCOMPONENT = new TextComponentString("(Cached)").setStyle(new Style()
			.setColor(TextFormatting.DARK_GRAY)
			.setItalic(true)
	);
	private static final ITextComponent UNSURE_TEXTCOMPONENT = new TextComponentString("(?)").setStyle(new Style()
			.setColor(TextFormatting.DARK_GRAY)
			.setItalic(true)
	);

	public static void drawChunkClaim(int claimDrawX, int claimDrawZ, CommonStateInfo owner, int cx, int cz, World world) {
		//fill colour
		Color4I colour = ClientStatesManager.isConflictState(owner) ? CONFLICT_COLOUR : owner.colour.getColor();
		Gui.drawRect(claimDrawX, claimDrawZ, claimDrawX + 16, claimDrawZ + 16,
				ColourUtils.colourAsInt(colour.redi(), colour.greeni(), colour.bluei(), 100));

		//draw borders with other control points
		BlockPos chunkCp = StatesManager.getChunkControlPointPos(cx, cz, world);
		if(chunkCp == null)
			return;

		int borderColour = ColourUtils.colourAsInt(0, 0, 0, 255);
		BlockPos otherCp = StatesManager.getChunkControlPointPos(cx-1, cz, world);
		if(!chunkCp.equals(otherCp)) {
			Gui.drawRect(claimDrawX, claimDrawZ, claimDrawX+1, claimDrawZ+16, borderColour);
		}

		otherCp = StatesManager.getChunkControlPointPos(cx+1, cz, world);
		if(!chunkCp.equals(otherCp)) {
			Gui.drawRect(claimDrawX+15, claimDrawZ, claimDrawX+16, claimDrawZ+16, borderColour);
		}

		otherCp = StatesManager.getChunkControlPointPos(cx, cz-1, world);
		if(!chunkCp.equals(otherCp)) {
			Gui.drawRect(claimDrawX, claimDrawZ, claimDrawX+16, claimDrawZ+1, borderColour);
		}

		otherCp = StatesManager.getChunkControlPointPos(cx, cz+1, world);
		if(!chunkCp.equals(otherCp)) {
			Gui.drawRect(claimDrawX, claimDrawZ+15, claimDrawX+16, claimDrawZ+16, borderColour);
		}
	}

	/**
	 * Draws a PermaloadedTileEntity display on the map.
	 * @param display The display to be drawn
	 * @param drawBlockX Where should the centre of this display be, in block coordinates
	 * @param drawBlockZ Where should the centre of this display be, in block coordinates
	 * @param zoomLevel Used to scale up the icons as we zoom out so they remain visible
	 */
	public static void drawPTEDisplay(PTEDisplay display, int drawBlockX, int drawBlockZ, double zoomLevel) {
		zoomLevel = Math.max(zoomLevel, 2.5d); //limit icon scaling once zoomed out enough
		//set up gl state
		GlStateManager.pushMatrix();
		GlStateManager.translate(drawBlockX+0.5d, drawBlockZ+0.5d, 0);
		GlStateManager.scale(2d/zoomLevel, 2d/zoomLevel, 1d);
		GlStateManager.disableLighting();
		//render display stack
		Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(display.displayStack, -8, -8);
		//tinting
		GlStateManager.depthFunc(516);
		Gui.drawRect(-8, -8, 8, 8, display.tint);
		GlStateManager.depthFunc(515);
		//reset gl state
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}

	public static void drawPTEHoverText(List<PTEDisplay> displays, int drawX, int drawZ, GuiScreen screen) {
		List<String> lines = new ArrayList<>();
		boolean first = true;
		for (PTEDisplay display : displays) {
			if(first) {
				first = false;
			} else {
				lines.add(""); //newline between each display's text
			}

			List<String> displayLines = display.getLines();
			for (int i = 0; i < displayLines.size(); i++) {
				if(i == 0 && display.isCached()) {
					lines.add(CACHED_DISPLAY_TEXTCOMPONENT.getFormattedText() +  " " + displayLines.get(i));
				} else {
					lines.add(displayLines.get(i));
				}
			}
		}
		screen.drawHoveringText(lines, drawX, drawZ);
	}

	public static void drawRegionTooltip(int hoveredBlockX, int hoveredBlockZ, CommonStateInfo stateInfo, World world, int drawX, int drawZ, GuiScreen screen) {
		BlockPos hoveredBlockPos = new BlockPos(hoveredBlockX, 64, hoveredBlockZ);
		BlockPos controlPointPos = StatesManager.getChunkControlPointPos(hoveredBlockPos, world);

		if(controlPointPos == null) {
			return; //not hovering a claimed chunk
		}

		List<String> lines = new ArrayList<>();
		TileEntityControlPoint cp;
		boolean isControlPointLoaded = world.isBlockLoaded(controlPointPos, false);
		if(!isControlPointLoaded //unloaded,
				|| (cp = StatesManager.getChunkControlPoint(controlPointPos, world)) == null) //..or could not find control point
		{
			lines.add("Claimed Region of " + stateInfo.name.getFormattedText());
		}
		else {
			lines.add(cp.getRegionName(true).getFormattedText());
		}

		//calculate stats from known and cached PTEDisplays
		float population = 0;
		float workingPopulation = 0;
		float industrialOutput = 0;
		float maxIndustrialOutput = 0;
		boolean usedCachedPop = false;
		boolean usedCachedIndustry = false;
		boolean hasAnyLabourContributions = false;
		Iterator<Map.Entry<BlockPos, PTEDisplay>> iter = getPTEDisplaysStreamIncludingCached().iterator();
		while (iter.hasNext()) {
			Map.Entry<BlockPos, PTEDisplay> kvp = iter.next();
			BlockPos pteControlPointPos = StatesManager.getChunkControlPointPos(kvp.getKey(), world);
			if(controlPointPos.equals(pteControlPointPos)) { //the PTE is within our hovered region
				hasAnyLabourContributions = true;
				float idealLabourContribution = kvp.getValue().idealLabourContribution;
				if(idealLabourContribution > 0) {
					population += idealLabourContribution;
					workingPopulation += kvp.getValue().labourContribution;
					if(kvp.getValue().isCached())
						usedCachedPop = true;
				} else {
					maxIndustrialOutput += -idealLabourContribution;
					industrialOutput += -kvp.getValue().labourContribution;
					if(kvp.getValue().isCached())
						usedCachedIndustry = true;
				}
			}
		}
		boolean sure = isControlPointLoaded; //are we sure that we know all the PTEs inside this region?
		if(sure) {
			int cpChunkX = controlPointPos.getX() >> 4;
			int cpChunkZ = controlPointPos.getZ() >> 4;
			BlockPos playerPos = screen.mc.player.getPosition();
			int playerChunkX = playerPos.getX() >> 4;
			int playerChunkZ = playerPos.getZ() >> 4;
			int dx = Math.abs(cpChunkX - playerChunkX);
			int dz = Math.abs(cpChunkZ - playerChunkZ);
			int claimRadius = ModConfig.controlPointClaimRadius;
			if(dx + claimRadius > PTEDisplaysSyncPacket.VIEW_DISTANCE || dz + claimRadius > PTEDisplaysSyncPacket.VIEW_DISTANCE)
				sure = false; //some chunks may be outside of our view distance
		}

		if (isControlPointLoaded || hasAnyLabourContributions) {
			lines.add(" - Working Population: " + workingPopulation + "/" + population
					+ (usedCachedPop ? " " + CACHED_DISPLAY_TEXTCOMPONENT.getFormattedText()
					: (sure ? "" : " " + UNSURE_TEXTCOMPONENT.getFormattedText()))
			);
			lines.add(" - Industrial Output: " + industrialOutput + "/" + maxIndustrialOutput
					+ (usedCachedIndustry ? " " + CACHED_DISPLAY_TEXTCOMPONENT.getFormattedText()
					: (sure ? "" : " " + UNSURE_TEXTCOMPONENT.getFormattedText()))
			);
		}

		screen.drawHoveringText(lines, drawX, drawZ);
	}

	public static void updatePTEDisplays(Map<BlockPos, PTEDisplay> newDisplays) {
		cacheCurrentPTEDisplays();
		//replace current displays with new displays
		PTE_DISPLAYS.clear();
		PTE_DISPLAYS.putAll(newDisplays);
		//ensure no overlap between cached displays and current displays
		for (BlockPos displayPos : newDisplays.keySet()) {
			PTE_DISPLAYS_CACHED.remove(displayPos);
		}

		//limit cache size
		synchronized (PTE_DISPLAYS_CACHED) {
			int toRemove = PTE_DISPLAYS_CACHED.size() - MAX_CACHE_SIZE;
			if(toRemove <= 0)
				return;
			List<BlockPos> removeList = new ArrayList<>();
			Iterator<BlockPos> iter = PTE_DISPLAYS_CACHED.keySet().iterator();
			while (toRemove > 0 && iter.hasNext()) {
				removeList.add(iter.next());
				toRemove--;
			}
			for (BlockPos blockPos : removeList) {
				PTE_DISPLAYS_CACHED.remove(blockPos);
			}
		}
	}

	public static void clearCachedPTEDisplays() {
		PTE_DISPLAYS_CACHED.clear();
	}

	private static void cacheCurrentPTEDisplays() {
		for (Map.Entry<BlockPos, PTEDisplay> kvp : PTE_DISPLAYS.entrySet()) {
			kvp.getValue().setCached();
			PTE_DISPLAYS_CACHED.put(kvp.getKey(), kvp.getValue());
		}
	}

	public static Stream<Map.Entry<BlockPos, PTEDisplay>> getPTEDisplaysStreamIncludingCached() {
		return Stream.concat(
				PTE_DISPLAYS.entrySet().stream(),
				PTE_DISPLAYS_CACHED.entrySet().stream()
		);
	}
}
