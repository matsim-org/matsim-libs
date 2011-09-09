package freight.listener;

import freight.DetailedCostStatusEvent;

public interface ShipperDetailedCostStatusHandler extends ShipperEventHandler{
	
	public void handleEvent(DetailedCostStatusEvent event);
	
	public void reset(int iteration);
	
	public void finish();
}
