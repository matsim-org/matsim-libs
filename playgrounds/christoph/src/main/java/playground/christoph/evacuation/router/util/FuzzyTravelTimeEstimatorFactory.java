/* *********************************************************************** *
 * project: org.matsim.*
 * FuzzyTravelTimeEstimatorFactory.java
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

package playground.christoph.evacuation.router.util;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;

import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

public class FuzzyTravelTimeEstimatorFactory implements TravelTimeFactory {
	
	private final Scenario scenario;
	private final TravelTime travelTime;
	private final AgentsTracker agentsTracker;
	private final VehiclesTracker vehiclesTracker;
	private final DistanceFuzzyFactorProviderFactory distanceFuzzyFactorProviderFactory;
	
	public FuzzyTravelTimeEstimatorFactory(Scenario scenario, TravelTime travelTime, AgentsTracker agentsTracker,
			VehiclesTracker vehiclesTracker) {
		this.scenario = scenario;
		this.travelTime = travelTime;
		this.agentsTracker = agentsTracker;
		this.vehiclesTracker = vehiclesTracker;

		this.distanceFuzzyFactorProviderFactory = new DistanceFuzzyFactorProviderFactory(scenario);
	}
	
	@Override
	public FuzzyTravelTimeEstimator createTravelTime() {
		return new FuzzyTravelTimeEstimator(scenario, travelTime, agentsTracker, vehiclesTracker, 
				distanceFuzzyFactorProviderFactory.createInstance());
	}
}