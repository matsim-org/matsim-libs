package example.lsp.simulationTrackers;

import org.matsim.contrib.freight.events.LSPTourStartEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPTourStartEventHandler;
import org.matsim.vehicles.Vehicle;

/*package-private*/ class TourStartHandler implements LSPTourStartEventHandler {

	private double vehicleFixedCosts;
		
	@Override
	public void reset(int iteration) {
		vehicleFixedCosts = 0;
	}

	@Override
	public void handleEvent(LSPTourStartEvent event) {
		vehicleFixedCosts = vehicleFixedCosts + ((Vehicle) event.getVehicle()).getType().getCostInformation().getFix();
	}

	public double getVehicleFixedCosts() {
		return vehicleFixedCosts;
	}

}
