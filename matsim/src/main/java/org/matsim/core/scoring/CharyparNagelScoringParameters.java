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

package org.matsim.core.scoring;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;

public class CharyparNagelScoringParameters implements MatsimParameters {
	public final TreeMap<String, ActivityUtilityParameters> utilParams = new TreeMap<String, ActivityUtilityParameters>();
	public final double marginalUtilityOfWaiting_s;
	public final double marginalUtilityOfLateArrival_s;
	public final double marginalUtilityOfEarlyDeparture_s;
	public final double marginalUtilityOfTraveling_s;
	public final double marginalUtilityOfTravelingPT_s; // public transport
	public final double marginalUtilityOfTravelingWalk_s;
	public final double marginalUtilityOfPerforming_s;

	@Deprecated
	public final double marginalUtilityOfDistanceCar;
	@Deprecated
	public final double marginalUtilityOfDistancePt;
	@Deprecated
	public final double marginalUtilityOfDistanceWalk;

	public final double abortedPlanScore;

	/** True if one at least one of marginal utilities for performing, waiting, being late or leaving early is not equal to 0. */
	public final boolean scoreActs;
	
	public CharyparNagelScoringParameters(final CharyparNagelScoringConfigGroup config) {
		marginalUtilityOfWaiting_s = config.getWaiting() / 3600.0;
		marginalUtilityOfLateArrival_s = config.getLateArrival() / 3600.0;
		marginalUtilityOfEarlyDeparture_s = config.getEarlyDeparture() / 3600.0;
		marginalUtilityOfTraveling_s = config.getTraveling() / 3600.0;
		marginalUtilityOfTravelingPT_s = config.getTravelingPt() / 3600.0;
		marginalUtilityOfTravelingWalk_s = config.getTravelingWalk() / 3600.0;
		marginalUtilityOfPerforming_s = config.getPerforming() / 3600.0;

		marginalUtilityOfDistanceCar = config.getMarginalUtlOfDistanceCar();
		marginalUtilityOfDistancePt = config.getMarginalUtlOfDistancePt();
		marginalUtilityOfDistanceWalk = config.getMarginalUtlOfDistanceWalk();

		abortedPlanScore = Math.min(
				Math.min(marginalUtilityOfLateArrival_s, marginalUtilityOfEarlyDeparture_s),
				Math.min(marginalUtilityOfTraveling_s, marginalUtilityOfWaiting_s)) * 3600.0 * 24.0; // SCENARIO_DURATION
		// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)

		scoreActs = ((marginalUtilityOfPerforming_s != 0) || (marginalUtilityOfWaiting_s != 0) ||
				(marginalUtilityOfLateArrival_s != 0) || (marginalUtilityOfEarlyDeparture_s != 0));

	
		for (ActivityParams params : config.getActivityParams()) {
			String type = params.getType();
			double priority = params.getPriority();
			double typDurationSecs = params.getTypicalDuration();
			ActivityUtilityParameters actParams = new ActivityUtilityParameters(type, priority, typDurationSecs);
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
