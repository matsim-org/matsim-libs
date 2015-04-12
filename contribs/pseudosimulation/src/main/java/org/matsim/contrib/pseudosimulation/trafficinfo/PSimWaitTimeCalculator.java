package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.controler.listeners.MobSimSwitcher;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;

/**
 * @author fouriep
 *         <P>
 *         Extends Ordonez's {@link WaitTimeStuckCalculator} to only handle
 *         events during QSim iterations.
 */
public class PSimWaitTimeCalculator extends WaitTimeStuckCalculator {

	public PSimWaitTimeCalculator(Population population,
			TransitSchedule transitSchedule, int timeSlot, int totalTime) {
		super(population, transitSchedule, timeSlot, totalTime);

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
	public void handleEvent(PersonDepartureEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

}
