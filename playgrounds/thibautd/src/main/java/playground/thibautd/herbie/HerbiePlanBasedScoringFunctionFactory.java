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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.facilities.ActivityFacilities;
import playground.thibautd.socnetsimusages.cliques.herbie.scoring.HerbieJointActivityScoringFunction;
import playground.thibautd.socnetsimusages.cliques.herbie.scoring.HerbieJointLegScoringFunction;

import java.util.TreeMap;

/**
 * @author thibautd
 */
public class HerbiePlanBasedScoringFunctionFactory implements ScoringFunctionFactory {
	private final Config config;
	private final HerbieConfigGroup ktiConfigGroup;
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final ActivityFacilities facilities;
	private final Network network;
	private final CharyparNagelScoringParameters params;
	
	public HerbiePlanBasedScoringFunctionFactory(
			final Config config, 
			final HerbieConfigGroup ktiConfigGroup,
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final ActivityFacilities facilities, 
			final Network network) {
		this.params = new CharyparNagelScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(null), config.scenario()).build();
		this.config = config;
		this.ktiConfigGroup = ktiConfigGroup;
		this.facilityPenalties = facilityPenalties;
		this.facilities = facilities;
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		final ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(
				// if no pick-up or drop off, behaviour the one of the "base" herbie
				// scoring.
				new HerbieJointActivityScoringFunction(
					person.getSelectedPlan(), 
					params,
					this.facilityPenalties,
					this.facilities,
					this.config));

		scoringFunctionAccumulator.addScoringFunction(
				new PlanBasedLegScoringFunction(
					person.getSelectedPlan(),
					//new HerbieLegWithNetworkPtDistanceScoringFunction(
					// this is the same as the classic herbie scoring function,
					// with handling of "joint" modes.
					new HerbieJointLegScoringFunction(
						person.getSelectedPlan(), 
						params,
						config,
						this.network,
						this.ktiConfigGroup)));

		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelMoneyScoring( params ) );

		scoringFunctionAccumulator.addScoringFunction(
				new CharyparNagelAgentStuckScoring( params ));
		
		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}
}

