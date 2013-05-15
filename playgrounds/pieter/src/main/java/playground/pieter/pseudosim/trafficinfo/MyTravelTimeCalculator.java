/**
 * 
 */
package playground.pieter.pseudosim.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;

/**
 * @author fouriep
 *
 */
public class MyTravelTimeCalculator extends TravelTimeCalculator {
	public MyTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network,ttconfigGroup.getTraveltimeBinSize(),50*3600, ttconfigGroup);
	}
	


	@Override
	public void reset(int iteration) {
		if (MobSimSwitcher.expensiveIter) {
			Logger.getLogger(this.getClass()).error("Calling reset on traveltimecalc");
			super.reset(iteration);
		}
	}

}
