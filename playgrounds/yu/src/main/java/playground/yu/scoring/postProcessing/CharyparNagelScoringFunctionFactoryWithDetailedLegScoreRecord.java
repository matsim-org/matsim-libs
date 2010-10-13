/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord.java
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

package playground.yu.scoring.postProcessing;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

public class CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord
		extends CharyparNagelScoringFunctionFactory {

	public CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord(
			CharyparNagelScoringConfigGroup config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator
				.addScoringFunction(new ActivityScoringFunction(plan,
						getParams()));

		LegScoringFunctionWithDetailedRecord legScoring = new LegScoringFunctionWithDetailedRecord(
				plan, getParams());
		scoringFunctionAccumulator.addScoringFunction(legScoring);

		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(
				getParams()));
		scoringFunctionAccumulator
				.addScoringFunction(new AgentStuckScoringFunction(getParams()));
		return scoringFunctionAccumulator;
	}
}
