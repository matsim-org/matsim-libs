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

package playground.meisterk.kti.scoring;

import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

import playground.meisterk.kti.config.KtiConfigGroup;


public class KTIYear3ScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final KtiConfigGroup ktiConfigGroup;
	
	public KTIYear3ScoringFunctionFactory(
			CharyparNagelScoringConfigGroup config, 
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final KtiConfigGroup ktiConfigGroup) {
		super(config);
		this.facilityPenalties = facilityPenalties;
		this.ktiConfigGroup = ktiConfigGroup;
	}

	public ScoringFunction getNewScoringFunction(PlanImpl plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, super.getParams(), this.facilityPenalties));
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(plan, super.getParams(), this.ktiConfigGroup));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.MoneyScoringFunction(super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction(super.getParams()));
		
		return scoringFunctionAccumulator;
		
	}

}
