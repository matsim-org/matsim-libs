/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringParameters.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.scoring;

import java.util.TreeMap;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.population.ActUtilityParameters;

public class CharyparNagelScoringParameters {
	public final TreeMap<String, ActUtilityParameters> utilParams = new TreeMap<String, ActUtilityParameters>();
	public final double marginalUtilityOfWaiting;
	public final double marginalUtilityOfLateArrival;
	public final double marginalUtilityOfEarlyDeparture;
	public final double marginalUtilityOfTraveling;
	public final double marginalUtilityOfTravelingPT; // public transport
	public final double marginalUtilityOfTravelingWalk;
	public final double marginalUtilityOfPerforming;
	public final double marginalUtilityOfDistance;
	public final double abortedPlanScore;

	/** True if one at least one of marginal utilities for performing, waiting, being late or leaving early is not equal to 0. */
	public final boolean scoreActs;
	
	public CharyparNagelScoringParameters(final CharyparNagelScoringConfigGroup config) {
		marginalUtilityOfWaiting = config.getWaiting() / 3600.0;
		marginalUtilityOfLateArrival = config.getLateArrival() / 3600.0;
		marginalUtilityOfEarlyDeparture = config.getEarlyDeparture() / 3600.0;
		marginalUtilityOfTraveling = config.getTraveling() / 3600.0;
		marginalUtilityOfTravelingPT = config.getTravelingPt() / 3600.0;
		marginalUtilityOfTravelingWalk = config.getTravelingWalk() / 3600.0;
		marginalUtilityOfPerforming = config.getPerforming() / 3600.0;

		marginalUtilityOfDistance = config.getMarginalUtlOfDistance();

		abortedPlanScore = Math.min(
				Math.min(marginalUtilityOfLateArrival, marginalUtilityOfEarlyDeparture),
				Math.min(marginalUtilityOfTraveling, marginalUtilityOfWaiting)) * 3600.0 * 24.0; // SCENARIO_DURATION
		// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)

		scoreActs = (marginalUtilityOfPerforming != 0 || marginalUtilityOfWaiting != 0 ||
				marginalUtilityOfLateArrival != 0 || marginalUtilityOfEarlyDeparture != 0);

	
		for (ActivityParams params : config.getActivityParams()) {
			String type = params.getType();
			double priority = params.getPriority();
			double typDurationSecs = params.getTypicalDuration();
			ActUtilityParameters actParams = new ActUtilityParameters(type, priority, typDurationSecs);
			if (params.getMinimalDuration() >= 0) {
				actParams.setMinimalDuration(params.getMinimalDuration());
			}
			if (params.getOpeningTime() >= 0) {
				actParams.setOpeningTime(params.getOpeningTime());
			}
			if (params.getLatestStartTime() >= 0) {
				actParams.setLatestStartTime(params.getLatestStartTime());
			}
			if (params.getEarliestEndTime() >= 0) {
				actParams.setEarliestEndTime(params.getEarliestEndTime());
			}
			if (params.getClosingTime() >= 0) {
				actParams.setClosingTime(params.getClosingTime());
			}
			utilParams.put(type, actParams);
		}

	}
	
}
