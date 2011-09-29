package city2000w.replanning;

import freight.ShipperImpl;

public interface ShipperPlanStrategyModule {
	
	public void prepareReplanning();
	
	public void handleActor(ShipperImpl shipper);
	
	public void finishReplanning();

}
