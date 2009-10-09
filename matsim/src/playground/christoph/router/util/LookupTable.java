package playground.christoph.router.util;

/*
 * LookupTable for TravelTimes and TravelCosts
 */
public interface LookupTable {
		
	public void updateLookupTable(double time);

	public void resetLookupTable();
	
	public double lastUpdate();
}
