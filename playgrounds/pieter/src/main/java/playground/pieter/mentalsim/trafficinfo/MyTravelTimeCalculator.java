/**
 * 
 */
package playground.pieter.mentalsim.trafficinfo;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

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
		if (iteration % 5 == 0) {
			super.reset(iteration);
		}
	}

}
