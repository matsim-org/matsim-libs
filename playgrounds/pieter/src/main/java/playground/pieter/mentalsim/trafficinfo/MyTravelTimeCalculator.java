/**
 * 
 */
package playground.pieter.mentalsim.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.pieter.mentalsim.controler.listeners.MobSimSwitcher;

/**
 * @author fouriep
 *
 */
public class MyTravelTimeCalculator extends TravelTimeCalculator {
	public MyTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, ttconfigGroup);
	}
	


	@Override
	public void reset(int iteration) {
		if (iteration % MobSimSwitcher.expensiveSimIters == 0) {
			Logger.getLogger(this.getClass()).error("Calling reset on traveltimecalc");
			super.reset(iteration);
		}
	}

}
