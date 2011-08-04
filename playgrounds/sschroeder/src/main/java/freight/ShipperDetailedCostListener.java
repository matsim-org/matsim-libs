package freight;

import freight.ShipperAgent.DetailedCost;

public interface ShipperDetailedCostListener {
	public void inform(DetailedCost detailedCost);
	
	public void reset(int iteration);
	
	public void finish();
}
