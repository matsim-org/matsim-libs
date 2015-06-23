/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.scoring.withAttrRecorder.leftTurn;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;

/**
 * @author yu
 * 
 */
public class CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty extends
		CharyparNagelScoringFunctionFactory {
	// private final Config config;
	private final AdditionalScoringParameters additionalParams;
	private Config config;

	public CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
			Config config, Network network) {
		super(config.planCalcScore(), network);
		this.config = config;
		additionalParams = new AdditionalScoringParameters(config);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		CharyparNagelScoringParameters params = CharyparNagelScoringParameters.getBuilder(config.planCalcScore()).createCharyparNagelScoringParameters();
		ScoringFunctionAccumulatorWithLeftTurnPenalty scoringFunctionAccumulator = new ScoringFunctionAccumulatorWithLeftTurnPenalty(
				params);
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelActivityScoring(params));
		scoringFunctionAccumulator
				.addScoringFunction(new LegScoringFunctionWithLeftTurnPenalty(
				// plan,
						params, network, additionalParams));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(
				params));
		scoringFunctionAccumulator
				.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		return scoringFunctionAccumulator;
	}

	public AdditionalScoringParameters getAdditionalParams() {
		return additionalParams;
	}
}
