package playground.ikaddoura.busCorridor.busCorridorWelfareAnalysis;

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


import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class MyScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	private PtLegHandler inVehWaitHandler;
	private double TRAVEL_PT_IN_VEHICLE;
	private double TRAVEL_PT_WAITING;
	private double monetaryCostPerKm;
	private double agentStuckScore;
	

	public MyScoringFunctionFactory(final PlanCalcScoreConfigGroup config, PtLegHandler inVehWaitHandler, double TRAVEL_PT_IN_VEHICLE, double TRAVEL_PT_WAITING, double monetaryCostPerKm, double agentStuckScore) {
		this.params = new CharyparNagelScoringParameters(config);
		this.inVehWaitHandler = inVehWaitHandler;
		this.TRAVEL_PT_IN_VEHICLE = TRAVEL_PT_IN_VEHICLE;
		this.TRAVEL_PT_WAITING = TRAVEL_PT_WAITING;
		this.monetaryCostPerKm = monetaryCostPerKm;
		this.agentStuckScore = agentStuckScore;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(new CarLegScoringFunction(person.getSelectedPlan(), params, this.monetaryCostPerKm));
		scoringFunctionAccumulator.addScoringFunction(new MyMoneyScoringFunction(params));
		scoringFunctionAccumulator.addScoringFunction(new MyAgentStuckScoringFunction(this.agentStuckScore));
		scoringFunctionAccumulator.addScoringFunction(new PtLegScoringFunction(person.getSelectedPlan(), inVehWaitHandler.getPersonId2InVehicleTime(), inVehWaitHandler.getPersonId2WaitingTime(), this.TRAVEL_PT_IN_VEHICLE, this.TRAVEL_PT_WAITING));
		scoringFunctionAccumulator.addScoringFunction(new MyActivityScoringFunction(params));
		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}
}
