package kaktusz.geopolitika.permaloaded.tileentities;

import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import kaktusz.geopolitika.integration.PTEDisplay;
import kaktusz.geopolitika.states.StatesManager;
import kaktusz.geopolitika.util.MessageUtils;
import net.minecraft.util.text.TextComponentString;

public interface UpkeepPTE extends PTEInterface {

	long getBaseUpkeep();

	/**
	 * Gets the true upkeep cost paid by this PTE, accounting for territory and labour.
	 */
	default long getUpkeepCost() {
		PermaloadedTileEntity pte = getPermaTileEntity();
		if(inInvalidTerritory()) {
			return 0; //no upkeep if we are in unclaimed territory
		}

		if(pte instanceof LabourConsumer) {
			LabourConsumer labourConsumer = (LabourConsumer) pte;
			if(labourConsumer.getLabourReceived() < labourConsumer.getLabourPerTick()) {
				return 0; //no upkeep if we have insufficient labour
			}
		}

		return getBaseUpkeep();
	}

	default boolean inInvalidTerritory() {
		PermaloadedTileEntity pte = getPermaTileEntity();
		ForgeTeam chunkOwner = StatesManager.getChunkOwner(pte.getPosition(), pte.getWorld());
		return !chunkOwner.isValid() || StatesManager.isConflictTeam(chunkOwner);
	}

	default void addUpkeepText(PTEDisplay display) {
		long upkeep = getUpkeepCost();
		boolean problem = false;
		if(upkeep == 0) {
			upkeep = getBaseUpkeep();
			problem = upkeep != 0; //we have a problem if our true upkeep is 0 but our base upkeep is not 0.
		}

		String text;
		if(upkeep > 0) {
			text = " - Upkeep: ";
		} else if (upkeep < 0) {
			text = " - Revenue: ";
		}
		else {
			return;
		}

		text += MessageUtils.CREDITS_FORMAT.format(upkeep) + "cr/h";
		if(problem) {
			text = new TextComponentString(text + " (not applicable)").setStyle(LabourConsumer.LABOUR_NOT_ENOUGH_STYLE).getFormattedText();
		}
		display.hoverText += "\n" + text;
	}
}
