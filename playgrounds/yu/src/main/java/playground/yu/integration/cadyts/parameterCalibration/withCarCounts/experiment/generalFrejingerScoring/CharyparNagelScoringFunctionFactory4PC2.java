package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalFrejingerScoring;

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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

public class CharyparNagelScoringFunctionFactory4PC2 implements
		ScoringFunctionFactory {
	private Config config;
	private Network network;

	public CharyparNagelScoringFunctionFactory4PC2(final Config config,
			final Network network) {
		this.config = config;
		this.network = network;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {
		CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(
				config.planCalcScore());
		ScoringFunctionAccumulator4PC2 scoringFunctionAccumulator = new ScoringFunctionAccumulator4PC2(
				params);
		scoringFunctionAccumulator
				.addScoringFunction(new ActivityScoringFunction(plan, params));
		scoringFunctionAccumulator
				.addScoringFunction(new LegScoringFunction4PC2(plan, config,
						network));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(
				params));
		scoringFunctionAccumulator
				.addScoringFunction(new AgentStuckScoringFunction(params));
		scoringFunctionAccumulator
				.addScoringFunction(new PathSizeScoringFunction(plan, network,
						config));
		return scoringFunctionAccumulator;
	}
}
