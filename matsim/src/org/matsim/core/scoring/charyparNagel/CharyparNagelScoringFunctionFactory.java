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

package org.matsim.core.scoring.charyparNagel;

import org.matsim.core.api.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * 
 * @author rashid_waraich
 */
public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;

	public CharyparNagelScoringFunctionFactory(final CharyparNagelScoringConfigGroup config) {
		this.params = new CharyparNagelScoringParameters(config);
	}

	/**
	 * puts the scoring functions together, which form the
	 * CharyparScoringFunction
	 * 
	 * @param plan
	 * @return
	 */
	public ScoringFunction getNewScoringFunction(Plan plan) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, params));

		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(plan, params));

		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));

		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));

		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}
}
