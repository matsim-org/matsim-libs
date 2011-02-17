/* *********************************************************************** *
 * project: org.matsim.*
 * FreeSpeedTravelTimeCalculatorFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.trafficmonitoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimFactory;

public class TravelTimeCollectorFactory implements MatsimFactory {

	public TravelTimeCollector createFreeSpeedTravelTimeCalculator(final Scenario scenario) {
		return new TravelTimeCollector(scenario, TravelTimeCollectorFactory.getNextId());
	}
	
	public TravelTimeCollector createFreeSpeedTravelTimeCalculator(final Network network, int numThreads) {
		return new TravelTimeCollector(network, numThreads, TravelTimeCollectorFactory.getNextId());
	}
	
	/*
	 * Each TravelTimeCollector gets a unique Id.
	 */
	private static int idCount = 0;
	private synchronized static int getNextId() {
		idCount++;
		return idCount;
	}
}