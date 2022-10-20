package kaktusz.geopolitika.integration;

import com.feed_the_beast.ftblib.lib.icon.Color4I;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.init.ModConfig;
import kaktusz.geopolitika.networking.PTEDisplaysSyncPacket;
import kaktusz.geopolitika.permaloaded.tileentities.LabourConsumer;
import kaktusz.geopolitika.states.ClientStatesManager;
import kaktusz.geopolitika.states.CommonStateInfo;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.tileentities.TileEntityControlPoint;
import kaktusz.geopolitika.util.ColourUtils;
import kaktusz.geopolitika.util.Vec2i;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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

//this should be converted to singleton at some point
@SideOnly(Side.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinimapIntegrationHelper {

	private static final Color4I CONFLICT_COLOUR = Color4I.rgba(255, 0, 0, 50);
	public static final Color4I HIGHLIGHT_DEFAULT_COLOUR = Color4I.WHITE.withAlpha(110);
	public static final short HIGHLIGHT_DEFAULT_FILL_OPACITY = 20;
	private static final ITextComponent CACHED_DISPLAY_TEXTCOMPONENT = new TextComponentString("(Cached)").setStyle(new Style()
			.setColor(TextFormatting.DARK_GRAY)
			.setItalic(true)
	);
	private static final ITextComponent UNSURE_TEXTCOMPONENT = new TextComponentString("(?)").setStyle(new Style()
			.setColor(TextFormatting.DARK_GRAY)
			.setItalic(true)
	);
	private static final Style NO_POP_OR_INDUSTRY_STYLE = new Style().setColor(TextFormatting.DARK_GRAY);

	private static final Map<BlockPos, PTEDisplay> PTE_DISPLAYS = new ConcurrentHashMap<>();
	private static final Map<ChunkPos, Map<BlockPos, PTEDisplay>> PTE_DISPLAYS_CACHED = new ConcurrentHashMap<>();
	@SuppressWarnings("UnstableApiUsage")
	private static final ListMultimap<Vec2i, PTEDisplay> PTE_DISPLAYS_SORTED = MultimapBuilder.hashKeys().arrayListValues().build();
	private static final int MAX_CACHE_SIZE = 10000;

	private static int cacheSize = 0;

	public static void drawChunkClaim(int claimDrawX, int claimDrawZ, double opacityFactor, CommonStateInfo owner, int cx, int cz, World world) {
		//fill colour
		Color4I colour = ClientStatesManager.isConflictState(owner) ? CONFLICT_COLOUR : owner.colour.getColor();
		Gui.drawRect(claimDrawX, claimDrawZ, claimDrawX + 16, claimDrawZ + 16,
				ColourUtils.colourAsInt(colour.redi(), colour.greeni(), colour.bluei(), (int) (opacityFactor*100)));

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

	public static void drawRadiusHighlight(int centreChunkX, int centreChunkZ, int radius, int colour, int fillOpacity) {
		int minBlockX = (centreChunkX - radius) << 4;
		int minBlockZ = (centreChunkZ - radius) << 4;
		int maxBlockX = (centreChunkX + radius + 1) << 4;
		int maxBlockZ = (centreChunkZ + radius + 1) << 4;

		//fill
		if(fillOpacity > 0) {
			int fillColour = ColourUtils.colourWithOpacity(colour, fillOpacity);
			Gui.drawRect(minBlockX+1, minBlockZ+1, maxBlockX-1, maxBlockZ-1, fillColour);
		}

		//outline
		Gui.drawRect(minBlockX, minBlockZ+1, minBlockX+1, maxBlockZ, colour);
		Gui.drawRect(maxBlockX-1, minBlockZ, maxBlockX, maxBlockZ-1, colour);
		Gui.drawRect(minBlockX, minBlockZ, maxBlockX-1, minBlockZ+1, colour);
		Gui.drawRect(minBlockX+1, maxBlockZ-1, maxBlockX, maxBlockZ, colour);
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

	public static void drawPTEHoverText(Iterable<PTEDisplay> displays, int drawX, int drawZ, GuiScreen screen) {
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
			//population
			String workingPopString = " - Working Population: " + workingPopulation + "/" + population;
			if(population == 0) {
				workingPopString = new TextComponentString(workingPopString).setStyle(
						NO_POP_OR_INDUSTRY_STYLE
				).getFormattedText();
			}

			lines.add(workingPopString
					+ (usedCachedPop ? " " + CACHED_DISPLAY_TEXTCOMPONENT.getFormattedText()
					: (sure ? "" : " " + UNSURE_TEXTCOMPONENT.getFormattedText()))
			);

			//industrial output
			String industrialOutputString = " - Industrial Output: " + industrialOutput + "/" + maxIndustrialOutput;
			if(maxIndustrialOutput == 0) {
				industrialOutputString = new TextComponentString(industrialOutputString).setStyle(
						NO_POP_OR_INDUSTRY_STYLE
				).getFormattedText();
			}
			else if(industrialOutput < maxIndustrialOutput) {
				industrialOutputString = new TextComponentString(industrialOutputString).setStyle(LabourConsumer
						.LABOUR_NOT_ENOUGH_STYLE
				).getFormattedText();
			}

			lines.add(industrialOutputString
					+ (usedCachedIndustry ? " " + CACHED_DISPLAY_TEXTCOMPONENT.getFormattedText()
					: (sure ? "" : " " + UNSURE_TEXTCOMPONENT.getFormattedText()))
			);
		}

		screen.drawHoveringText(lines, drawX, drawZ);
	}

	public static void updatePTEDisplays(Map<BlockPos, PTEDisplay> newDisplays, int cacheClearChunkDistance) {
		long startTime = System.nanoTime();
		cacheCurrentPTEDisplays(cacheClearChunkDistance);
		//replace current displays with new displays
		PTE_DISPLAYS.clear();
		PTE_DISPLAYS.putAll(newDisplays);

		//update sorted displays
		PTE_DISPLAYS_SORTED.clear();
		Iterator<Map.Entry<BlockPos, PTEDisplay>> displaysUnsorted = MinimapIntegrationHelper.getPTEDisplaysStreamIncludingCached()
				.iterator();
		while (displaysUnsorted.hasNext()) {
			Map.Entry<BlockPos, PTEDisplay> next = displaysUnsorted.next();
			PTE_DISPLAYS_SORTED.put(new Vec2i(next.getKey().getX(), next.getKey().getZ()), next.getValue());
		}
		for (Vec2i key : PTE_DISPLAYS_SORTED.keySet()) {
			PTE_DISPLAYS_SORTED.get(key).sort(PTEDisplay::compareTo);
		}

		double elapsed = (System.nanoTime() - startTime)/1000000d;
		Geopolitika.logger.info("Update PTE Displays took " + elapsed + "ms");
	}

	public static void clearCachedPTEDisplays() {
		PTE_DISPLAYS_CACHED.clear();
		cacheSize = 0;
	}

	/**
	 * Caches current PTE Displays and removes any cached displays which share a chunk with the new displays.
	 */
	private static void cacheCurrentPTEDisplays(int cacheClearChunkDistance) {
		ChunkPos playerPos = new ChunkPos(Minecraft.getMinecraft().player.getPosition());
		for (Map.Entry<BlockPos, PTEDisplay> kvp : PTE_DISPLAYS.entrySet()) {
			ChunkPos displayChunk = new ChunkPos(kvp.getKey());
			int dx = Math.abs(playerPos.x - displayChunk.x);
			int dz = Math.abs(playerPos.z - displayChunk.z);
			if(dx <= cacheClearChunkDistance || dz <= cacheClearChunkDistance)
				continue; //don't cache overwritten chunks

			kvp.getValue().setCached();
			PTE_DISPLAYS_CACHED.computeIfAbsent(displayChunk, cp -> new ConcurrentHashMap<>())
					.put(kvp.getKey(), kvp.getValue());
			cacheSize++;
		}

		//un-cache any overwritten chunks
		for (int dx = -cacheClearChunkDistance; dx <= cacheClearChunkDistance; dx++) {
			for (int dz = -cacheClearChunkDistance; dz <= cacheClearChunkDistance; dz++) {
				ChunkPos chunkPos = new ChunkPos(playerPos.x + dx, playerPos.z + dz);
				Map<BlockPos, PTEDisplay> removed = PTE_DISPLAYS_CACHED.remove(chunkPos);
				if(removed != null) {
					cacheSize -= removed.size();
				}
			}
		}

		//limit cache size
		synchronized (PTE_DISPLAYS_CACHED) {
			if(cacheSize <= MAX_CACHE_SIZE)
				return;
			List<ChunkPos> removeList = new ArrayList<>();
			Iterator<Map.Entry<ChunkPos, Map<BlockPos, PTEDisplay>>> iter = PTE_DISPLAYS_CACHED.entrySet().iterator();
			while (cacheSize > MAX_CACHE_SIZE && iter.hasNext()) {
				Map.Entry<ChunkPos, Map<BlockPos, PTEDisplay>> chunkDisplays = iter.next();
				removeList.add(chunkDisplays.getKey()); //remove all cached displays in chunk
				int chunkDisplaysCount = chunkDisplays.getValue().size();
				cacheSize -= chunkDisplaysCount;
			}
			for (ChunkPos chunkPos : removeList) {
				PTE_DISPLAYS_CACHED.remove(chunkPos);
			}
		}
	}

	public static Stream<Map.Entry<BlockPos, PTEDisplay>> getPTEDisplaysStreamIncludingCached() {
		return Stream.concat(
				PTE_DISPLAYS.entrySet().stream(),
				PTE_DISPLAYS_CACHED.values().stream().flatMap(chunk -> chunk.entrySet().stream())
		);
	}

	public static ListMultimap<Vec2i, PTEDisplay> getSortedPTEDisplaysIncludingCached() {
		return PTE_DISPLAYS_SORTED;
	}
}
