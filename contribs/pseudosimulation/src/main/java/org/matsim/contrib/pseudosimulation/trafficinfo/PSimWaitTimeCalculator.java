package org.matsim.contrib.pseudosimulation.trafficinfo;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.core.api.experimental.events.EventsManager;


/**
 * @author fouriep
 *         <P>
 *         Extends Ordonez's {@link WaitTimeStuckCalculator} to only handle
 *         events during QSim iterations.
 */
public class PSimWaitTimeCalculator extends WaitTimeCalculatorSerializable {
	private final MobSimSwitcher switcher;

	@Inject
	public PSimWaitTimeCalculator(Scenario scenario,
								  MobSimSwitcher switcher, EventsManager eventsManager) {
		super(scenario.getTransitSchedule(),scenario.getConfig());
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
	public void handleEvent(PersonDepartureEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}



}
