/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorWithBufferFactory.java
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

package playground.christoph.multimodal.router.costcalculator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

public class TravelTimeCalculatorWithBufferFactory implements TravelTimeCalculatorFactory {

	public TravelTimeCalculator createTravelTimeCalculator(Network network, TravelTimeCalculatorConfigGroup group) {
		return new TravelTimeCalculatorWithBuffer(network, group);
	}

} 