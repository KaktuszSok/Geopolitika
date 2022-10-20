package kaktusz.geopolitika.permaloaded.tileentities;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public interface LabourSupplier extends PTEInterface {

	String NO_LABOUR_TEXT = "\n" + new TextComponentString(" - Labour provided: 0.0/0.0").setStyle(new Style()
			.setColor(TextFormatting.RED)
	).getFormattedText();

	double getAvailableLabour();
	void setAvailableLabour(double labour);

	/**
	 * Labour per tick this supplier generates if all its needs are met.
	 */
	double getIdealLabourPerTick();
	int getLabourTier();

	default String getLabourHoverText(double labourProvidedLastTick) {
		if(getIdealLabourPerTick() == 0) {
			return NO_LABOUR_TEXT;
		}
		return "\n - Labour provided: " + labourProvidedLastTick + "/" + getIdealLabourPerTick();
	}

	/**
	 * Requests labour from this supplier, draining its avaialable labour this tick by up to amount if the tier matches.
	 * @return amount of labour drained.
	 */
	default double requestLabour(double amount, int tier) {
		if(tier != getLabourTier())
			return 0;
		double drained = Math.min(amount, getAvailableLabour()); //drain no more than the requested amount or the labour available
		setAvailableLabour(getAvailableLabour() - drained);
		return drained;
	}
}
