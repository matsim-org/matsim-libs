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
import org.matsim.core.api.network.Network;


/**
 * 
 * @author dgrether
 *
 */
public class TravelTimeCalculatorBuilder {

	private static final Logger log = Logger
			.getLogger(TravelTimeCalculatorBuilder.class);
	
	private TravelTimeCalculatorConfigGroup travelTimeCalcConfigGroup;

	public TravelTimeCalculatorBuilder(TravelTimeCalculatorConfigGroup group){
		this.travelTimeCalcConfigGroup = group;
	}
	
	public AbstractTravelTimeCalculator createTravelTimeCalculator(Network network, int endTime) {
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		AbstractTravelTimeCalculator calculator = null;
		
		if ("TravelTimeCalculatorHashMap".equals(travelTimeCalcConfigGroup.getTravelTimeCalculatorType())) {
			factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		} else if (!"TravelTimeCalculatorArray".equals(travelTimeCalcConfigGroup.getTravelTimeCalculatorType())) {
			throw new RuntimeException(travelTimeCalcConfigGroup.getTravelTimeCalculatorType() + " is unknown!");
		}
		
		if ("experimental_LastMile".equals(this.travelTimeCalcConfigGroup.getTravelTimeAggregatorType())) {
			factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
			log.warn("Using experimental TravelTimeAggregator! \nIf this was not intendet please remove the travelTimeAggregator entry in the controler section in your config.xml!");
		} else if (!"optimistic".equals(this.travelTimeCalcConfigGroup.getTravelTimeAggregatorType())) {
			throw new RuntimeException(this.travelTimeCalcConfigGroup.getTravelTimeAggregatorType() + " is unknown!");
		}
		calculator = new TravelTimeCalculator(network, this.travelTimeCalcConfigGroup.getTraveltimeBinSize(), 
				endTime, factory, this.travelTimeCalcConfigGroup);
		

		return calculator;
	}
	
}
