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
package playground.yu.scoring.withAttrRecorder;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;

public class CharyparNagelScoringFunctionFactory4AttrRecorder extends
		CharyparNagelScoringFunctionFactory {

	private PlanCalcScoreConfigGroup config;

	public CharyparNagelScoringFunctionFactory4AttrRecorder(
			final PlanCalcScoreConfigGroup config, final Network network) {
		super(config, network);
		this.config = config;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		CharyparNagelScoringParameters params = CharyparNagelScoringParameters.getBuilder(config).create();
		ScoringFunctionAccumulatorWithAttrRecorder scoringFunctionAccumulator = new ScoringFunctionAccumulatorWithAttrRecorder(
				params);
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelActivityScoring(params));
		scoringFunctionAccumulator
				.addScoringFunction(new LegScoringFunctionWithAttrRecorder(
				// plan,
						params, network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(
				params));
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		return scoringFunctionAccumulator;
	}
}
