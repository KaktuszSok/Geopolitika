package kaktusz.geopolitika.integration;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.icon.Color4I;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.ColourUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MinimapIntegrationHelper {

	private static final Color4I CONFLICT_COLOUR = Color4I.rgba(255, 0, 0, 50);

	public static void drawChunkClaim(int claimDrawX, int claimDrawZ, ForgeTeam owner, int cx, int cz, World world) {
		//fill colour
		Color4I colour = StatesManager.isConflictTeam(owner) ? CONFLICT_COLOUR : owner.getColor().getColor();
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
}
