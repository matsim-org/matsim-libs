package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;


public class PSimStopStopTimeCalculator extends StopStopTimeCalculatorSerializable {
	private final MobSimSwitcher switcher;

	@Inject
	PSimStopStopTimeCalculator(Scenario scenario, MobSimSwitcher switcher, EventsManager eventsManager) {
		super(scenario.getTransitSchedule(),scenario.getConfig()  );
		this.switcher = switcher;
		eventsManager.addHandler(this);
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
