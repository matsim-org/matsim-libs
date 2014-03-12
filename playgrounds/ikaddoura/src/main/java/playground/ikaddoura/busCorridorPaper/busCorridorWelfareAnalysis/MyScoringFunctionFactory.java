package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

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


import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class MyScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	private final PtLegHandler ptLegHandler;
	private final double TRAVEL_PT_IN_VEHICLE;
	private final double TRAVEL_PT_WAITING;
	private final double TRAVEL_PT_ACCESS;
	private final double TRAVEL_PT_EGRESS;
	private final double stuckScore;
	private Network network;
	

	public MyScoringFunctionFactory(final PlanCalcScoreConfigGroup planCalcScoreConfigGroup, Network network, PtLegHandler ptLegHandler, double travelingPtInVehicle, double travelingPtWaiting, double stuckScore, double access, double egress) {
		this.params = new CharyparNagelScoringParameters(planCalcScoreConfigGroup);
		this.network = network;
		this.ptLegHandler = ptLegHandler;
		this.TRAVEL_PT_IN_VEHICLE = travelingPtInVehicle;
		this.TRAVEL_PT_WAITING = travelingPtWaiting;
		this.stuckScore = stuckScore;
		this.TRAVEL_PT_ACCESS = access;
		this.TRAVEL_PT_EGRESS = egress;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		
		Id personId = person.getId();
		Map <Id, Double> personId2InVehTime = this.ptLegHandler.getPersonId2InVehicleTime();
		Map <Id, Double> personId2WaitingTime = this.ptLegHandler.getPersonId2WaitingTime();

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(new MyLegScoringFunction(this.params, network, personId, this.ptLegHandler, personId2InVehTime, personId2WaitingTime, this.TRAVEL_PT_IN_VEHICLE, this.TRAVEL_PT_WAITING, this.TRAVEL_PT_ACCESS, this.TRAVEL_PT_EGRESS));
		scoringFunctionAccumulator.addScoringFunction(new MyMoneyScoringFunction(this.params));
		scoringFunctionAccumulator.addScoringFunction(new MyAgentStuckScoringFunction(this.stuckScore));
		scoringFunctionAccumulator.addScoringFunction(new MyActivityScoringFunction(this.params));
		return scoringFunctionAccumulator;
	}
}
