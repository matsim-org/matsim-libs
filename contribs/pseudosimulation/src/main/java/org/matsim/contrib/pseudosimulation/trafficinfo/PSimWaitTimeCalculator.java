package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.contrib.pseudosimulation.RunPSim;
import org.matsim.core.config.Config;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;

/**
 * @author fouriep
 *         <P>
 *         Extends Ordonez's {@link WaitTimeStuckCalculator} to only handle
 *         events during QSim iterations.
 */
public class PSimWaitTimeCalculator extends WaitTimeCalculatorSerializable {
	private final RunPSim.MobSimSwitcher switcher;
	public PSimWaitTimeCalculator(
			TransitSchedule transitSchedule, Config config,
								  RunPSim.MobSimSwitcher switcher) {
		super(transitSchedule,config);
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
