package playground.ikaddoura.busCorridor.version7test;

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


import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class MyScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	private Map<Id, Double> personId2WaitingTime = new HashMap<Id,Double>();
	private Map<Id, Double> personId2InVehicleTime = new HashMap<Id,Double>();

	public MyScoringFunctionFactory(final PlanCalcScoreConfigGroup config) {
		this.params = new CharyparNagelScoringParameters(config);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		System.out.println("scoring function factory: Scoring of "+plan.getPerson().getId());
		System.out.println("in scoring function Factory: "+this.personId2WaitingTime);

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new MyLegScoringFunction(plan, params, personId2WaitingTime, personId2InVehicleTime));
		scoringFunctionAccumulator.addScoringFunction(new MyMoneyScoringFunction(params));
//		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));
//		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, params));

		return scoringFunctionAccumulator;
	}

	public CharyparNagelScoringParameters getParams() {
		return params;
	}

	/**
	 * @return the personId2WaitingTime
	 */
	public Map<Id, Double> getPersonId2WaitingTime() {
		return personId2WaitingTime;
	}

	/**
	 * @param personId2WaitingTime the personId2WaitingTime to set
	 */
	public void setPersonId2WaitingTime(Map<Id, Double> personId2WaitingTime) {
		this.personId2WaitingTime = personId2WaitingTime;
	}

	public void setPersonId2InVehicleTime(Map<Id, Double> personId2InVehicleTime) {
		this.personId2InVehicleTime = personId2InVehicleTime;
	}

	public Map<Id, Double> getPersonId2InVehicleTime() {
		return personId2InVehicleTime;
	}

}
