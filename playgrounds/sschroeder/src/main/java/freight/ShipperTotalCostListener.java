package freight;

import freight.ShipperAgent.TotalCost;

public interface ShipperTotalCostListener {
	public void inform(TotalCost totalCost);
	
	public void reset(int iteration);
	
	public void finish();
}
