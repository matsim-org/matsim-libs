package playground.ikaddoura.busCorridor.finalDyn2;

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


import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;

public class MyScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	private PtLegHandler inVehWaitHandler;
	private double TRAVEL_PT_IN_VEHICLE;
	private double TRAVEL_PT_WAITING;
	protected final double monetaryCostPerKm;

	public MyScoringFunctionFactory(final PlanCalcScoreConfigGroup config, PtLegHandler inVehWaitHandler, double TRAVEL_PT_IN_VEHICLE, double TRAVEL_PT_WAITING, double monetaryCostPerKm) {
		this.params = new CharyparNagelScoringParameters(config);
		this.inVehWaitHandler = inVehWaitHandler;
		this.TRAVEL_PT_IN_VEHICLE = TRAVEL_PT_IN_VEHICLE;
		this.TRAVEL_PT_WAITING = TRAVEL_PT_WAITING;
		this.monetaryCostPerKm = monetaryCostPerKm;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(new CarLegScoringFunction(plan, params, this.monetaryCostPerKm));
		scoringFunctionAccumulator.addScoringFunction(new MyMoneyScoringFunction(params));
//		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
//		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(params));
		
		scoringFunctionAccumulator.addScoringFunction(new PtLegScoringFunction(plan, inVehWaitHandler.getPersonId2InVehicleTime(), inVehWaitHandler.getPersonId2WaitingTime(), this.TRAVEL_PT_IN_VEHICLE, this.TRAVEL_PT_WAITING));
		
		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}
}
