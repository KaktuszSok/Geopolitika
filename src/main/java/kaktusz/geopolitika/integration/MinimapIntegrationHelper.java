package kaktusz.geopolitika.integration;

import com.feed_the_beast.ftblib.lib.icon.Color4I;
import kaktusz.geopolitika.Geopolitika;
import kaktusz.geopolitika.permaloaded.tileentities.PTEDisplay;
import kaktusz.geopolitika.states.ClientStatesManager;
import kaktusz.geopolitika.states.CommonStateInfo;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.ColourUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MinimapIntegrationHelper {

	private static final Color4I CONFLICT_COLOUR = Color4I.rgba(255, 0, 0, 50);
	@SideOnly(Side.CLIENT)
	private static final Map<BlockPos, PTEDisplay> PTE_DISPLAYS = new ConcurrentHashMap<>();

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
	 * @param drawX Where should the centre of this display be
	 * @param drawZ Where should the centre of this display be
	 * @param zoomLevel Used to scale up the icons as we zoom out so they remain visible
	 */
	public static void drawPTEDisplay(PTEDisplay display, int drawX, int drawZ, double zoomLevel) {
		zoomLevel = Math.max(zoomLevel, 4.0d); //limit icon scaling once zoomed out enough
		//set up gl state
		GlStateManager.pushMatrix();
		GlStateManager.translate(drawX+0.5d, drawZ+0.5d, 0);
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

			lines.addAll(display.getLines());
		}
		screen.drawHoveringText(lines, drawX, drawZ);
	}

	@SideOnly(Side.CLIENT)
	public static Map<BlockPos, PTEDisplay> getPTEDisplaysMap() {
		return PTE_DISPLAYS;
	}
}
