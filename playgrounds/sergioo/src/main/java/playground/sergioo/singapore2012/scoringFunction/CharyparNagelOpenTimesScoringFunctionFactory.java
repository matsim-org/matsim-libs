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

package playground.sergioo.singapore2012.scoringFunction;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * Generates {@link CharyparNagelOpenTimesActivityScoring}s.
 * 
 * @author meisterk
 */
public class CharyparNagelOpenTimesScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
    private Scenario scenario;

    public CharyparNagelOpenTimesScoringFunctionFactory(final PlanCalcScoreConfigGroup config, final Scenario scenario) {
		this.params = new CharyparNagelScoringParameters(config);
		this.scenario = scenario;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelOpenTimesActivityScoring(plan, params, ((ScenarioImpl) scenario).getActivityFacilities()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		return scoringFunctionAccumulator;
	}

	
	
}
