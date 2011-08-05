package freight.listener;

import freight.ShipperAgent;
import freight.ShipperAgent.TotalCost;

public interface ShipperTotalCostListener {
	public void inform(TotalCost totalCost);
	
	public void reset(int iteration);
	
	public void finish();
}
