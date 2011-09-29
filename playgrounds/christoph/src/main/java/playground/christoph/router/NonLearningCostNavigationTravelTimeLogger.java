/* *********************************************************************** *
 * project: org.matsim.*
 * NonLearningCostNavigationTravelTimeLogger.java
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


package playground.christoph.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.util.TravelTime;

public class NonLearningCostNavigationTravelTimeLogger extends CostNavigationTravelTimeLogger {

	private final double trust;
	
	public NonLearningCostNavigationTravelTimeLogger(Population population,
			Network network, TravelTime travelTime, double trust) {
		super(population, network, travelTime);
		this.trust = trust;
	}
	
	public double getTrust(Id personId) {
		return trust;
	}

}
