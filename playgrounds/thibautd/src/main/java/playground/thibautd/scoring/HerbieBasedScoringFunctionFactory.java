/* *********************************************************************** *
 * project: org.matsim.*
 * HerbieBasedScoringFunctionFactory.java
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
package playground.thibautd.scoring;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.scoring.HerbieScoringFunctionFactory;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

/**
 * Based on {@link herbie.running.scoring.HerbieScoringFunctionFactory}.
 * Currently only a wrapper around the Herbie factory; in the future,
 * special parts related to car-pooling should be included.
 *
 * @author thibautd
 */
public class HerbieBasedScoringFunctionFactory implements ScoringFunctionFactory {
	private final ScoringFunctionFactory delegate;

	public HerbieBasedScoringFunctionFactory(
			final Config config,
			final Scenario scenario) {
		delegate = new HerbieScoringFunctionFactory(
			config, 
			new HerbieConfigGroup(),
			new TreeMap<Id, FacilityPenalty>(),
			((ScenarioImpl) scenario).getActivityFacilities(), 
			scenario.getNetwork()); 
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {
		return delegate.createNewScoringFunction( plan );
	}
}

