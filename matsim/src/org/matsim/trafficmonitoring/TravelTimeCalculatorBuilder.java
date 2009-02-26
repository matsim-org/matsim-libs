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
package org.matsim.trafficmonitoring;

import org.apache.log4j.Logger;
import org.matsim.config.groups.ControlerConfigGroup;
import org.matsim.network.NetworkLayer;


/**
 * 
 * @author dgrether
 *
 */
public class TravelTimeCalculatorBuilder {

	private static final Logger log = Logger
			.getLogger(TravelTimeCalculatorBuilder.class);
	
	private ControlerConfigGroup controlerConfigGroup;

	public TravelTimeCalculatorBuilder(ControlerConfigGroup group){
		this.controlerConfigGroup = group;
	}
	
	public TravelTimeCalculator createTravelTimeCalculator(NetworkLayer network, int endTime) {
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		
		if ("TravelTimeCalculatorHashMap".equals(controlerConfigGroup.getTravelTimeCalculatorType())) {
			factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		} else if (!"TravelTimeCalculatorArray".equals(controlerConfigGroup.getTravelTimeCalculatorType())) {
			throw new RuntimeException(controlerConfigGroup.getTravelTimeCalculatorType() + " is unknown!");
		}
		
		if ("experimental_LastMile".equals(this.controlerConfigGroup.getTravelTimeAggregatorType())) {
			factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
			log.warn("Using experimental TravelTimeAggregator! \nIf this was not intendet please remove the travelTimeAggregator entry in the controler section in your config.xml!");
		} else if (!"optimistic".equals(this.controlerConfigGroup.getTravelTimeAggregatorType())) {
			throw new RuntimeException(this.controlerConfigGroup.getTravelTimeAggregatorType() + " is unknown!");
		}

		return new TravelTimeCalculator(network, this.controlerConfigGroup.getTraveltimeBinSize(), endTime, factory);		
	}
	
}
