/* *********************************************************************** *
 * project: org.matsim.*
 * LeastCostPathCalculatorFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * Example(s):<ul>
 * <li> {@link tutorial.programming.example21tutorialTUBclass.leastCostPath.RunLeastCostPathCalculatorExample}
 * </ul>
 *
 */
public interface LeastCostPathCalculatorFactory extends MatsimFactory, MatsimExtensionPoint {
	
	/**
	 * Creational method, since Network may not be exactly the scenario's network
	 */
	
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes);

}
