package kaktusz.geopolitika.permaloaded.tileentities;

public interface LabourSupplier extends PTEInterface {

	double getAvailableLabour();
	void setAvailableLabour(double labour);

	/**
	 * Labour per tick this supplier generates if all its needs are met.
	 */
	double getIdealLabourPerTick();
	int getLabourTier();

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
