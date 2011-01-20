/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.yu.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

/**
 * change scoring function, because "walk"-mode will be implemented
 *
 * @author yu
 */
public class CharyparNagelScoringFunctionFactoryWithWalk implements
		ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	private final double offsetWalk;

	public CharyparNagelScoringFunctionFactoryWithWalk(
			final PlanCalcScoreConfigGroup config, final double offsetWalk) {
		this.params = new CharyparNagelScoringParameters(config);
		this.offsetWalk = offsetWalk;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator
				.addScoringFunction(new ActivityScoringFunction(plan, params));
		scoringFunctionAccumulator
				.addScoringFunction(new LegScoringFunctionWithWalk(
						plan, params, this.offsetWalk));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(
				params));
		scoringFunctionAccumulator
				.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}

}
