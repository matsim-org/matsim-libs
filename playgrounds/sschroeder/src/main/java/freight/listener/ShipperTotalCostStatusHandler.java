package freight.listener;


public interface ShipperTotalCostStatusHandler extends ShipperEventHandler{
	
	public void handleEvent(ShipperTotalCostStatusEvent event);
	
	public void reset(int iteration);
	
	public void finish();
}
