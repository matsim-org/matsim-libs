package playground.pieter.pseudosim.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.sergioo.singapore2012.transitRouterVariable.StopStopTimeCalculator;

public class PseudoSimStopStopTimeCalculator extends StopStopTimeCalculator {

	public PseudoSimStopStopTimeCalculator(TransitSchedule transitSchedule, Vehicles vehicles,
			int timeSlot, int totalTime) {
		super(transitSchedule, vehicles, timeSlot, totalTime);
	}
	@Override
	public void reset(int iteration) {
		if (MobSimSwitcher.isMobSimIteration) {
			Logger.getLogger(this.getClass()).error("Calling reset on traveltimecalc");
			super.reset(iteration);
		}
	}

}
