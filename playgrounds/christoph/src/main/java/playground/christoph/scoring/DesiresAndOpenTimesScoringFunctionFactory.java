/* *********************************************************************** *
 * project: org.matsim.*
 * DesiresAndOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

/**
 * 
 * @author cdobler
 */
public class DesiresAndOpenTimesScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
    private Scenario scenario;

    public DesiresAndOpenTimesScoringFunctionFactory(final PlanCalcScoreConfigGroup config, final Scenario scenario) {
		this.params = CharyparNagelScoringParameters.getBuilder(config).createCharyparNagelScoringParameters();
		this.scenario = scenario;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new DesiresAndOpenTimesActivityScoring(person.getSelectedPlan(), params, ((ScenarioImpl) scenario).getActivityFacilities()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		return scoringFunctionAccumulator;
	}

}