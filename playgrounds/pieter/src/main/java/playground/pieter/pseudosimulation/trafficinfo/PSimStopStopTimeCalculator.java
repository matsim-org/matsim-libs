package playground.pieter.pseudosimulation.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.pieter.pseudosimulation.controler.listeners.MobSimSwitcher;
import playground.sergioo.singapore2012.transitRouterVariable.StopStopTimeCalculator;

public class PSimStopStopTimeCalculator extends StopStopTimeCalculator {

	public PSimStopStopTimeCalculator(TransitSchedule transitSchedule,
			Vehicles vehicles, int timeSlot, int totalTime) {
		super(transitSchedule, vehicles, timeSlot, totalTime);
	}

	@Override
	public void reset(int iteration) {
		if (MobSimSwitcher.isQSimIteration) {
			Logger.getLogger(this.getClass()).error(
					"Calling reset on traveltimecalc");
			super.reset(iteration);
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

}
