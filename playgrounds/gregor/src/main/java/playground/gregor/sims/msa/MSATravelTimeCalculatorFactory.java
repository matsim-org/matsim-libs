/* *********************************************************************** *
 * project: org.matsim.*
 * MSATravelTimeCalculatorFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.gregor.sims.msa;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.OptimisticTravelTimeAggregator;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

public class MSATravelTimeCalculatorFactory implements
		TravelTimeCalculatorFactory {
	
	private static final Logger log = Logger.getLogger(MSATravelTimeCalculatorFactory.class);

	@Override
	public TravelTimeCalculator createTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup group) {
		TravelTimeCalculator calculator = new TravelTimeCalculator(network, group);
		
		// set travelTimeData factory
		if ("TravelTimeCalculatorArray".equals(group.getTravelTimeCalculatorType())) {
			
			
		} else if ("TravelTimeCalculatorHashMap".equals(group.getTravelTimeCalculatorType())) {
			throw new RuntimeException(group.getTravelTimeCalculatorType() + " is not allowed for MSATraveltimeCalculator!");
		} else if ("TravelTimeCalculatorMSAHashMap".equals(group.getTravelTimeCalculatorType())) {
			calculator.setTravelTimeDataFactory(new MSATravelTimeDataHashMapFactory(network,group.getTraveltimeBinSize()));
		} else {
			throw new RuntimeException(group.getTravelTimeCalculatorType() + " is unknown!");
		}
		
		// set travelTimeAggregator
		if ("optimistic".equals(group.getTravelTimeAggregatorType())) {
			calculator.setTravelTimeAggregator(new OptimisticTravelTimeAggregator(calculator.getNumSlots(), calculator.getTimeSlice()));
		} else if ("experimental_LastMile".equals(group.getTravelTimeAggregatorType())) {
			calculator.setTravelTimeAggregator(new PessimisticTravelTimeAggregator(calculator.getNumSlots(), calculator.getTimeSlice()));
			log.warn("Using experimental TravelTimeAggregator! \nIf this was not intendet please remove the travelTimeAggregator entry in the controler section in your config.xml!");
		} else {
			throw new RuntimeException(group.getTravelTimeAggregatorType() + " is unknown!");
		}
		
		return calculator;
	}

}
