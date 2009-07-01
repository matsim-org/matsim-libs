/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3ScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

public class KTIYear3ScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;

	private ActivityScoringFunction activities = null;
	
	public KTIYear3ScoringFunctionFactory(CharyparNagelScoringConfigGroup config, final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		super(config);
		this.facilityPenalties = facilityPenalties;
	}

	public ScoringFunction getNewScoringFunction(PlanImpl plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		this.activities = new ActivityScoringFunction(plan, super.getParams(), this.facilityPenalties);
		scoringFunctionAccumulator.addScoringFunction(this.activities);
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.LegScoringFunction(plan, super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.MoneyScoringFunction(super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction(super.getParams()));
		
		return scoringFunctionAccumulator;
		
	}

	public ActivityScoringFunction getActivities() {
		return activities;
	}

}
