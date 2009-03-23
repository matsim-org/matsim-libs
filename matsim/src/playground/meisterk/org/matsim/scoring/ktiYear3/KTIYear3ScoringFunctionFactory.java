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

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionAccumulator;

public class KTIYear3ScoringFunctionFactory extends org.matsim.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;

	public KTIYear3ScoringFunctionFactory(CharyparNagelScoringConfigGroup config, final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		super(config);
		this.facilityPenalties = facilityPenalties;
	}

	public ScoringFunction getNewScoringFunction(Plan plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, super.getParams(), this.facilityPenalties));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.scoring.charyparNagel.LegScoringFunction(plan, super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.scoring.charyparNagel.MoneyScoringFunction(super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.scoring.charyparNagel.AgentStuckScoringFunction(super.getParams()));
		
		return scoringFunctionAccumulator;
		
	}

}
