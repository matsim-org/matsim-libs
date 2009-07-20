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
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.locationchoice.facilityload.FacilityPenalty;


public class KTIYear3ScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final KTIScoringParameters ktiScoringParameters;
	
	private ActivityScoringFunction activities = null;
	
	public KTIYear3ScoringFunctionFactory(
			CharyparNagelScoringConfigGroup config, 
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final Module ktiConfigGroup) {
		super(config);
		this.facilityPenalties = facilityPenalties;
		this.ktiScoringParameters = new KTIScoringParameters(ktiConfigGroup);
	}

	public ScoringFunction getNewScoringFunction(PlanImpl plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		this.activities = new ActivityScoringFunction(plan, super.getParams(), this.facilityPenalties);
		scoringFunctionAccumulator.addScoringFunction(this.activities);
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(plan, super.getParams(), this.ktiScoringParameters));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.MoneyScoringFunction(super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction(super.getParams()));
		
		return scoringFunctionAccumulator;
		
	}

	public ActivityScoringFunction getActivities() {
		return activities;
	}

	public class KTIScoringParameters {

		public static final String CONST_BIKE = "constBike";
		
		/**
		 * Constant for scoring the mode "bike".
		 * <br>
		 * Mode "bike" is usually evaluated with a travel time based on crow fly distance, just as mode "walk".
		 * It is penalized with an additional constant to make it less attractive for short distances
		 * value of constant depends on the average speeds of modes "bike" and "walk", as well as the desired leg distance
		 * where "walk" and "bike" modes gain the same score
		 */
		private double constBike = -1.0;

		public KTIScoringParameters(Module ktiConfigGroup) {
			if (ktiConfigGroup != null) {
				this.constBike = Double.parseDouble(ktiConfigGroup.getValue(KTIScoringParameters.CONST_BIKE));
			}
		}

		public double getConstBike() {
			return constBike;
		}
		
	}

	public KTIScoringParameters getKtiScoringParameters() {
		return ktiScoringParameters;
	}
	
}
