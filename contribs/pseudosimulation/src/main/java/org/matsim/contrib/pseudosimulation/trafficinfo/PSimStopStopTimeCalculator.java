package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.pseudosimulation.RunPSim;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;

public class PSimStopStopTimeCalculator extends StopStopTimeCalculatorSerializable {
	private final RunPSim.MobSimSwitcher switcher;
	public PSimStopStopTimeCalculator(TransitSchedule transitSchedule,
			 int timeSlot, int totalTime, RunPSim.MobSimSwitcher switcher) {
		super(transitSchedule,  timeSlot, totalTime);
		this.switcher = switcher;
	}

	@Override
	public void reset(int iteration) {
		if (switcher.isQSimIteration()) {
			Logger.getLogger(this.getClass()).error(
					"Calling reset on traveltimecalc");
			super.reset(iteration);
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}

}
