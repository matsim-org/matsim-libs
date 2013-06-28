package playground.ikaddoura.optimization.scoring;

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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


public class OptimizationScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	private final PtLegHandler ptLegHandler;
	private final double TRAVEL_PT_ACCESS;
	private final double TRAVEL_PT_EGRESS;
	private final double STUCKING_SCORE;
	private Network network;
	

	public OptimizationScoringFunctionFactory(final PlanCalcScoreConfigGroup planCalcScoreConfigGroup, Network network, PtLegHandler ptLegHandler, double travelingPtInVehicle, double travelingPtWaiting, double stuckScore, double access, double egress) {
		this.params = new CharyparNagelScoringParameters(planCalcScoreConfigGroup);
		this.network = network;
		this.ptLegHandler = ptLegHandler;
		this.STUCKING_SCORE = stuckScore;
		this.TRAVEL_PT_ACCESS = access;
		this.TRAVEL_PT_EGRESS = egress;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		
		Id personId = plan.getPerson().getId();
	
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(new OptimizationLegScoringFunction(this.params, network, personId, this.ptLegHandler, this.TRAVEL_PT_ACCESS, this.TRAVEL_PT_EGRESS));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
		scoringFunctionAccumulator.addScoringFunction(new OptimizationAgentStuckScoringFunction(this.params, this.STUCKING_SCORE));
		scoringFunctionAccumulator.addScoringFunction(new OptimizationActivityScoringFunction(this.params));
		
		return scoringFunctionAccumulator;
	}
}
