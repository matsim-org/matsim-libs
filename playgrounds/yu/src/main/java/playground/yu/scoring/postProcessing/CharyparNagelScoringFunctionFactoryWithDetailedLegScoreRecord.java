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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;

public class CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord
		extends CharyparNagelScoringFunctionFactory {

	public CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord(
			PlanCalcScoreConfigGroup config, Network network) {
		super(config, network);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelActivityScoring(getParams()));

		LegScoringFunctionWithDetailedRecord legScoring = new LegScoringFunctionWithDetailedRecord(
				plan, getParams(), network);
		scoringFunctionAccumulator.addScoringFunction(legScoring);

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(
				getParams()));
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelAgentStuckScoring(getParams()));
		return scoringFunctionAccumulator;
	}
}
