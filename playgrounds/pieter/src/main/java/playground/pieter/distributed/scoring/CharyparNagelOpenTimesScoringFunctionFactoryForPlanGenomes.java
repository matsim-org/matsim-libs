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

package playground.pieter.distributed.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import playground.singapore.scoring.CharyparNagelOpenTimesActivityScoring;
import playground.singapore.scoring.SingaporeFareScoring;

/**
 * Generates {@link playground.singapore.scoring.CharyparNagelOpenTimesActivityScoring}s.
 *
 * @author meisterk
 */
public class CharyparNagelOpenTimesScoringFunctionFactoryForPlanGenomes implements ScoringFunctionFactory {

    private final boolean SingaporeScenario;
    private CharyparNagelScoringParameters params = null;
    private Scenario scenario;
	private PlanCalcScoreConfigGroup config;

    public CharyparNagelOpenTimesScoringFunctionFactoryForPlanGenomes(final PlanCalcScoreConfigGroup config, final Scenario scenario, boolean SingaporeScenario) {
    	this.config = config;
		this.scenario = scenario;
        this.SingaporeScenario = SingaporeScenario;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		if (this.params == null) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */
			this.params = new CharyparNagelScoringParameters(this.config);
		}
		SumScoringFunctionForPlanGenomes sumScoringFunction = new SumScoringFunctionForPlanGenomes(person);
		sumScoringFunction.addScoringFunction(new CharyparNagelOpenTimesActivityScoring(params, scenario.getActivityFacilities()));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
        if(SingaporeScenario)
		    sumScoringFunction.addScoringFunction(new SingaporeFareScoring(person.getSelectedPlan(), scenario.getTransitSchedule(), scenario.getNetwork()));
		return sumScoringFunction;
	}

	
	
}