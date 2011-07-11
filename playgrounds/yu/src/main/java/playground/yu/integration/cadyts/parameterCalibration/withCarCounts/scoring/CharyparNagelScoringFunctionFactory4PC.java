/* *********************************************************************** *
 * project: org.matsim.*
 * DummyCharyparNagelScoringFunctionFactory4PC.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.scoring.ScoringFunctionAccumulator4PC;

public class CharyparNagelScoringFunctionFactory4PC extends
		CharyparNagelScoringFunctionFactory {
	public CharyparNagelScoringFunctionFactory4PC(
			PlanCalcScoreConfigGroup config) {
		super(config);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		CharyparNagelScoringParameters params = getParams();
		ScoringFunctionAccumulator4PC scoringFunctionAccumulator = new ScoringFunctionAccumulator4PC(
				params);
		scoringFunctionAccumulator
				.addScoringFunction(new ActivityScoringFunction(plan, params));
		scoringFunctionAccumulator
				.addScoringFunction(new LegScoringFunction4PC(plan, params));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(
				params));
		scoringFunctionAccumulator
				.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}
}
