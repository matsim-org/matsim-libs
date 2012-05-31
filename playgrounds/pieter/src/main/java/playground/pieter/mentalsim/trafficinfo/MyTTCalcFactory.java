package playground.pieter.mentalsim.trafficinfo;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

import playground.pieter.mentalsim.trafficinfo.MyTravelTimeCalculator;

public class MyTTCalcFactory implements TravelTimeCalculatorFactory {

	@Override
	public TravelTimeCalculator createTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup group) {
		return new MyTravelTimeCalculator(network, group);
	}

}
