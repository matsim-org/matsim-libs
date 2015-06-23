/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3ScoringFunctionFactory.java
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

package playground.wrashid.parkingSearch.withindayFW.kti;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.facilities.ActivityFacilities;

import playground.meisterk.kti.config.KtiConfigGroup;


public class KTIYear3ScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {

    private final Scenario scenario;
	private final KtiConfigGroup ktiConfigGroup;
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final ActivityFacilities facilities;
	private Config config;

	public KTIYear3ScoringFunctionFactory(
			final Scenario scenario,
			final KtiConfigGroup ktiConfigGroup,
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final ActivityFacilities facilities) {
		super(scenario.getConfig().planCalcScore(), scenario.getNetwork());
		this.scenario = scenario;
		this.config = scenario.getConfig();
		this.ktiConfigGroup = ktiConfigGroup;
		this.facilityPenalties = facilityPenalties;
		this.facilities = facilities;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(
				person.getSelectedPlan(),
				CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters(),
				this.facilityPenalties,
				this.facilities));
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(
				person.getSelectedPlan(),
				CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters(),
				scenario.getConfig(),
                scenario.getNetwork(),
				this.ktiConfigGroup));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelMoneyScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters()));
		
		return scoringFunctionAccumulator;
	}

}
