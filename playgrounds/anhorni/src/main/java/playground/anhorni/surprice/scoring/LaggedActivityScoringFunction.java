/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ActivityUtilityParameters;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunction;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.anhorni.surprice.Surprice;

public class LaggedActivityScoringFunction extends CharyparNagelOpenTimesScoringFunction {
	
	private CharyparNagelScoringParameters params;
	private double votFactor;
	private Config config;
		
	public LaggedActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, final Config config, ActivityFacilities facilities, double votFactor) {
		super(plan, params, facilities);
		super.reset();
		this.params = params;
		this.config = config;
		this.votFactor = votFactor;
	}
	
	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {
		
		double f = 1.0;
		if (Boolean.parseBoolean(this.config.findParam(Surprice.SURPRICE_RUN, "useVoT"))) {
			f = this.votFactor;
		}

		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		double tmpScore = 0.0;
		double[] openingInterval = this.getOpeningInterval(act);
		double openingTime = openingInterval[0];
		double closingTime = openingInterval[1];

		double activityStart = arrivalTime;
		double activityEnd = departureTime;

		if ((openingTime >=  0) && (arrivalTime < openingTime)) {
			activityStart = openingTime;
		}
		if ((closingTime >= 0) && (closingTime < departureTime)) {
			activityEnd = closingTime;
		}
		if ((openingTime >= 0) && (closingTime >= 0)
				&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
			// agent could not perform action
			activityStart = departureTime;
			activityEnd = departureTime;
		}
		double duration = activityEnd - activityStart;

		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			tmpScore += this.params.marginalUtilityOfWaiting_s * f * (activityStart - arrivalTime);
		}

		// disutility if too late
		double latestStartTime = actParams.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += this.params.marginalUtilityOfLateArrival_s * f * (activityStart - latestStartTime);
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = actParams.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = this.params.marginalUtilityOfPerforming_s * f * typicalDuration
					* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration());
			double utilWait = this.params.marginalUtilityOfWaiting_s * f * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			tmpScore += 2 * this.params.marginalUtilityOfLateArrival_s * f * Math.abs(duration);
		}

		// disutility if stopping too early
		double earliestEndTime = actParams.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * f * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting_s * f * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = actParams.getMinimalDuration();
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * f * (minimalDuration - duration);
		}
		return tmpScore;
	}
}
