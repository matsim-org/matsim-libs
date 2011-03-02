/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorFactoryImpl
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

	@Override
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
		AbstractTravelTimeAggregator travelTimeAggregator;
		if ("optimistic".equals(group.getTravelTimeAggregatorType())) {
			travelTimeAggregator = new OptimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice);
			calculator.setTravelTimeAggregator(travelTimeAggregator);
		} else if ("experimental_LastMile".equals(group.getTravelTimeAggregatorType())) {
			travelTimeAggregator = new PessimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice);
			calculator.setTravelTimeAggregator(travelTimeAggregator);
			log.warn("Using experimental TravelTimeAggregator! \nIf this was not intended please remove the travelTimeAggregator entry in the controler section in your config.xml!");
		} else {
			throw new RuntimeException(group.getTravelTimeAggregatorType() + " is unknown!");
		}

		//TODO: make this configurable via the config file
		TravelTimeGetter travelTimeGetter = new AveragingTravelTimeGetter();	// by default
//		TravelTimeGetter travelTimeGetter = new LinearInterpolatingTravelTimeGetter(calculator.getNumSlots(), calculator.getTimeSlice());
		travelTimeAggregator.setTravelTimeGetter(travelTimeGetter);
		
		return calculator;
	}
	
}
