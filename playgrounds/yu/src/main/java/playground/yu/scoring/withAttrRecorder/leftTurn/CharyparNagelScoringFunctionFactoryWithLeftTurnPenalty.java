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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

/**
 * @author yu
 * 
 */
public class CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty extends
		CharyparNagelScoringFunctionFactory {
	// private final Config config;
	private final AdditionalScoringParameters additionalParams;

	public CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
			Config config, Network network) {
		super(config.planCalcScore(), network);
		// this.config = config;
		additionalParams = new AdditionalScoringParameters(config);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		CharyparNagelScoringParameters params = getParams();
		ScoringFunctionAccumulatorWithLeftTurnPenalty scoringFunctionAccumulator = new ScoringFunctionAccumulatorWithLeftTurnPenalty(
				params);
		scoringFunctionAccumulator
				.addScoringFunction(new ActivityScoringFunction(params));
		scoringFunctionAccumulator
				.addScoringFunction(new LegScoringFunctionWithLeftTurnPenalty(
				// plan,
						params, network, additionalParams));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(
				params));
		scoringFunctionAccumulator
				.addScoringFunction(new AgentStuckScoringFunction(params));
		return scoringFunctionAccumulator;
	}

	public AdditionalScoringParameters getAdditionalParams() {
		return additionalParams;
	}
}
