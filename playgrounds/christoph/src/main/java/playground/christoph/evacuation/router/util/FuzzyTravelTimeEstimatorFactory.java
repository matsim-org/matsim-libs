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
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;

import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

public class FuzzyTravelTimeEstimatorFactory implements PersonalizableTravelTimeFactory {

	private final Scenario scenario;
	private final PersonalizableTravelTimeFactory timeFactory;
	private final AgentsTracker agentsTracker;
	private final VehiclesTracker vehiclesTracker;
	
	public FuzzyTravelTimeEstimatorFactory(Scenario scenario, PersonalizableTravelTimeFactory timeFactory, AgentsTracker agentsTracker,
			VehiclesTracker vehiclesTracker) {
		this.scenario = scenario;
		this.timeFactory = timeFactory;
		this.agentsTracker = agentsTracker;
		this.vehiclesTracker = vehiclesTracker;
	}
	
	@Override
	public FuzzyTravelTimeEstimator createTravelTime() {
		return new FuzzyTravelTimeEstimator(scenario, timeFactory.createTravelTime(), agentsTracker, vehiclesTracker);
	}
	
}