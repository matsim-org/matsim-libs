package testMutualreplanningWithOfferUpdate;

import lsp.events.TourStartEvent;
import lsp.events.TourStartEventHandler;

public class TourStartHandler implements TourStartEventHandler{

	private double vehicleFixedCosts;
		
	@Override
	public void reset(int iteration) {
		vehicleFixedCosts = 0;
	}

	@Override
	public void handleEvent(TourStartEvent event) {
		vehicleFixedCosts = vehicleFixedCosts + event.getVehicle().getVehicleType().getVehicleCostInformation().fix;
	}

	public double getVehicleFixedCosts() {
		return vehicleFixedCosts;
	}

}
