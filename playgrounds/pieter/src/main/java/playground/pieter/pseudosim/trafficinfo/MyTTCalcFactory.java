package playground.pieter.pseudosim.trafficinfo;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

import playground.pieter.pseudosim.trafficinfo.MyTravelTimeCalculator;

public class MyTTCalcFactory implements TravelTimeCalculatorFactory {
	MyTravelTimeCalculator mtc;
	@Override
	public TravelTimeCalculator createTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup group) {
		if(mtc == null){
			mtc = new MyTravelTimeCalculator(network, group);
			
		}
		return mtc;
	}

}
