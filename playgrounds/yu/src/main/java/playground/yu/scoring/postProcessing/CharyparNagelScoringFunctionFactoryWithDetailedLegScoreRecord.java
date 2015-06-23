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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord
		extends CharyparNagelScoringFunctionFactory {

	private PlanCalcScoreConfigGroup config;

	public CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord(
			PlanCalcScoreConfigGroup config, Network network) {
		super(config, network);
		this.config = config;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelActivityScoring(CharyparNagelScoringParameters.getBuilder(config).create()));

		LegScoringFunctionWithDetailedRecord legScoring = new LegScoringFunctionWithDetailedRecord(
				person.getSelectedPlan(), CharyparNagelScoringParameters.getBuilder(config).create(), network);
		scoringFunctionAccumulator.addScoringFunction(legScoring);

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(
				CharyparNagelScoringParameters.getBuilder(config).create()));
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config).create()));
		return scoringFunctionAccumulator;
	}
}
