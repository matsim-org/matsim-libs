/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;

/**
 * Generates {@link FacilityOpeningIntervalCalculator}s.
 * 
 * @author meisterk
 */
public final class CharyparNagelOpenTimesScoringFunctionFactory implements ScoringFunctionFactory {

	private final ScoringParametersForPerson params;
    private final Scenario scenario;

    public CharyparNagelOpenTimesScoringFunctionFactory(
			final ScoringParametersForPerson params,
			final Scenario scenario) {
		this.params = params;
		this.scenario = scenario;
	}

	public CharyparNagelOpenTimesScoringFunctionFactory(
			final Scenario scenario) {
		this.params = new SubpopulationScoringParameters( scenario );
		this.scenario = scenario;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		final ScoringParameters parameters = params.getScoringParameters( person );

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters, new FacilityOpeningIntervalCalculator(scenario.getActivityFacilities())));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, scenario.getNetwork(), scenario.getConfig().transit().getTransitModes()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		sumScoringFunction.addScoringFunction(new ScoreEventScoring());
		return sumScoringFunction;
	}

	
	
}
