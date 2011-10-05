package playground.ikaddoura.busCorridor.version3_moneyEvents_scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 * MyScoringFunctionFactory.java
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


import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class MyScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;

	public MyScoringFunctionFactory(final PlanCalcScoreConfigGroup config) {
		this.params = new CharyparNagelScoringParameters(config);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(new MyLegScoringFunction(plan, params));
		scoringFunctionAccumulator.addScoringFunction(new MyMoneyScoringFunction(params));

//		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
//		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, params));

		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}
}
