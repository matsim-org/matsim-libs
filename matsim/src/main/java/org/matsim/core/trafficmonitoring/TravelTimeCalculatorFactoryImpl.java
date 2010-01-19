/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.trafficmonitoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;


/**
 * @author dgrether
 * @author mrieser
 */
public class TravelTimeCalculatorFactoryImpl implements TravelTimeCalculatorFactory {

	private static final Logger log = Logger.getLogger(TravelTimeCalculatorFactoryImpl.class);

	public TravelTimeCalculator createTravelTimeCalculator(final Network network, final TravelTimeCalculatorConfigGroup group) {
		TravelTimeCalculator calculator = new TravelTimeCalculator(network, group);
		
		// set travelTimeData factory
		if ("TravelTimeCalculatorArray".equals(group.getTravelTimeCalculatorType())) {
			calculator.setTravelTimeDataFactory(new TravelTimeDataArrayFactory(network, calculator.numSlots));
		} else if ("TravelTimeCalculatorHashMap".equals(group.getTravelTimeCalculatorType())) {
			calculator.setTravelTimeDataFactory(new TravelTimeDataHashMapFactory(network));
		} else {
			throw new RuntimeException(group.getTravelTimeCalculatorType() + " is unknown!");
		}
		
		// set travelTimeAggregator
		if ("optimistic".equals(group.getTravelTimeAggregatorType())) {
			calculator.setTravelTimeAggregator(new OptimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice));
		} else if ("experimental_LastMile".equals(group.getTravelTimeAggregatorType())) {
			calculator.setTravelTimeAggregator(new PessimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice));
			log.warn("Using experimental TravelTimeAggregator! \nIf this was not intendet please remove the travelTimeAggregator entry in the controler section in your config.xml!");
		} else {
			throw new RuntimeException(group.getTravelTimeAggregatorType() + " is unknown!");
		}
		
		return calculator;
	}
	
}
