/* *********************************************************************** *
 * project: org.matsim.*
 * HerbiePlanBasedScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.herbie;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.scoring.ActivityScoringFunction;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

import herbie.running.scoring.LegScoringFunction;

/**
 * @author thibautd
 */
public class HerbiePlanBasedScoringFunctionFactory implements ScoringFunctionFactory {
	private final Config config;
	private final HerbieConfigGroup ktiConfigGroup;
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final ActivityFacilities facilities;
	private Network network;
	private CharyparNagelScoringParameters params;
	
	public HerbiePlanBasedScoringFunctionFactory(
			final Config config, 
			final HerbieConfigGroup ktiConfigGroup,
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final ActivityFacilities facilities, 
			final Network network) {
		this.params = new CharyparNagelScoringParameters( config.planCalcScore() );
		this.config = config;
		this.ktiConfigGroup = ktiConfigGroup;
		this.facilityPenalties = facilityPenalties;
		this.facilities = facilities;
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(
				new ActivityScoringFunction(
					plan, 
					params,
					this.facilityPenalties,
					this.facilities,
					this.config));

		scoringFunctionAccumulator.addScoringFunction(
				new PlanBasedLegScoringFunction(
					plan,
					//new HerbieLegWithNetworkPtDistanceScoringFunction(
					new LegScoringFunction(
						plan, 
						params,
						config,
						this.network,
						this.ktiConfigGroup)));

		scoringFunctionAccumulator.addScoringFunction(
				new MoneyScoringFunction( params ) );

		scoringFunctionAccumulator.addScoringFunction(
				new AgentStuckScoringFunction( params ));
		
		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}
}

