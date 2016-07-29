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

package playground.singapore.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.FacilityOpeningIntervalCalculator;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

/**
 * Generates {@link CharyparNagelOpenTimesActivityScoring}s.
 * 
 * @author meisterk
 */
public class CharyparNagelOpenTimesScoringFunctionFactory implements ScoringFunctionFactory {

    private Scenario scenario;
	private PlanCalcScoreConfigGroup config;
	private CharyparNagelScoringParametersForPerson parametersForPerson;

    public CharyparNagelOpenTimesScoringFunctionFactory(final PlanCalcScoreConfigGroup config, final Scenario scenario) {
    	this.config = config;
		this.scenario = scenario;
		this.parametersForPerson = new SubpopulationCharyparNagelScoringParameters( scenario );
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		final CharyparNagelScoringParameters params = parametersForPerson.getScoringParameters( person );
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		//sumScoringFunction.addScoringFunction(new CharyparNagelOpenTimesActivityScoring(params, scenario.getActivityFacilities()));
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params, new FacilityOpeningIntervalCalculator(scenario.getActivityFacilities())));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		sumScoringFunction.addScoringFunction(new SingaporeFareScoring(person.getSelectedPlan(), scenario.getTransitSchedule(), scenario.getNetwork()));
		return sumScoringFunction;
	}

	
	
}