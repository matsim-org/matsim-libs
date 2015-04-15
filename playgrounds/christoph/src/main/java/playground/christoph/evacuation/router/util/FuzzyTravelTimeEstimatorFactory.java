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
import org.matsim.withinday.mobsim.MobsimDataProvider;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.AgentsTracker;

import javax.inject.Provider;

public class FuzzyTravelTimeEstimatorFactory implements Provider<TravelTime> {
	
	private final Scenario scenario;
	private final TravelTime travelTime;
	private final AgentsTracker agentsTracker;
	private final MobsimDataProvider mobsimDataProvider;
	private final DistanceFuzzyFactorProviderFactory distanceFuzzyFactorProviderFactory;
	
	public FuzzyTravelTimeEstimatorFactory(Scenario scenario, TravelTime travelTime, AgentsTracker agentsTracker,
			MobsimDataProvider mobsimDataProvider) {
		this.scenario = scenario;
		this.travelTime = travelTime;
		this.agentsTracker = agentsTracker;
		this.mobsimDataProvider = mobsimDataProvider;

		this.distanceFuzzyFactorProviderFactory = new DistanceFuzzyFactorProviderFactory(scenario);
	}
	
	@Override
	public FuzzyTravelTimeEstimator get() {
		return new FuzzyTravelTimeEstimator(scenario, travelTime, agentsTracker, mobsimDataProvider, 
				distanceFuzzyFactorProviderFactory.createInstance(),
				EvacuationConfig.fuzzyTravelTimeEstimatorRandomSeed + EvacuationConfig.deterministicRNGOffset);
	}
}