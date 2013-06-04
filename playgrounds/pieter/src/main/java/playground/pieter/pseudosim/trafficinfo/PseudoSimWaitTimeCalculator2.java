package playground.pieter.pseudosim.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.sergioo.singapore2012.transitRouterVariable.WaitTimeStuckCalculator;

public class PseudoSimWaitTimeCalculator2 extends WaitTimeStuckCalculator {

	public PseudoSimWaitTimeCalculator2(Population population,
			TransitSchedule transitSchedule, int timeSlot, int totalTime) {
		super(population, transitSchedule, timeSlot, totalTime);
		
	}
	@Override
	public void reset(int iteration) {
		if (MobSimSwitcher.isMobSimIteration) {
			Logger.getLogger(this.getClass()).error("Calling reset on traveltimecalc");
			super.reset(iteration);
		}
	}

}
