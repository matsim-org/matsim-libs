package playground.ikaddoura.parkAndRide.strategyTest;

/* *********************************************************************** *
 * project: org.matsim.*
 * MyScoringFunctionFactory.java
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


import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

import playground.ikaddoura.busCorridor.busCorridorWelfareAnalysis.MyActivityScoringFunction;

public class ParkAndRideScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
    protected Network network;


	public ParkAndRideScoringFunctionFactory(final PlanCalcScoreConfigGroup config, Network network) {
		this.params = new CharyparNagelScoringParameters(config);
        this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(params, network));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(params));
//		scoringFunctionAccumulator.addScoringFunction(new MyAgentStuckScoringFunction(-999999));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new ParkAndRideActivityScoring(params));
		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}
}
