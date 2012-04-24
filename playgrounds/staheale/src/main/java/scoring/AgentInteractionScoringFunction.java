/* *********************************************************************** *
 * project: org.matsim.*
 * AgentInteractionScoringFunction.java
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

package scoring;

import java.util.TreeMap;
import occupancy.FacilityOccupancy;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.scoring.ActivityUtilityParameters;
import org.matsim.core.scoring.CharyparNagelOpenTimesScoringFunction;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import facilities.CreateFacilityAttributes;

/*
 * Scoring function taking agent interaction into account
 */
public class AgentInteractionScoringFunction extends CharyparNagelOpenTimesScoringFunction {
	//private final TreeMap<Id, FacilityOccupancy> facilityOccupancies;
	private CharyparNagelScoringParameters params;
	private FacilityOccupancy occupancies;
	private CreateFacilityAttributes capacities;
	

	public AgentInteractionScoringFunction(final Plan plan, final CharyparNagelScoringParameters params, final TreeMap<Id, FacilityOccupancy> facilityOccupancies, final ActivityFacilities facilities, final TreeMap<Id, CreateFacilityAttributes> facilityAttributes) {
		super(plan, params, facilities);
		this.params = params;
	}
	
	@Override
	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {

		ActivityUtilityParameters params = this.params.utilParams.get(act.getType());
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
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

		// utility of performing an action, duration is >= 1, thus log is no problem ----------------
		double typicalDuration = params.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
					* Math.log((duration / 3600.0) / params.getZeroUtilityDuration());



			double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));

		} else {
			tmpScore += 2*this.params.marginalUtilityOfLateArrival_s*Math.abs(duration);
		}


		// DISUTILITIES: ==============================================================================
		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			tmpScore += this.params.marginalUtilityOfWaiting_s * (activityStart - arrivalTime);
		}

		// disutility if too late
		double latestStartTime = params.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = params.getMinimalDuration();
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
		}
				
		// ------------disutilities of agent interaction----------- 
		String cap = capacities.getCapacity();
		double capacity = Double.parseDouble(cap);
		double occupancy = occupancies.getOccupancyPerHour(activityStart);
		double load = occupancy/capacity;
		
		// disutility of agent interaction underarousal
		String thresholdUnderArousal = capacities.getLowerBound();
		double lowerBound = Double.parseDouble(thresholdUnderArousal);
		String lowerMUtility = capacities.getLowerMarginalUtility();
		double lowerMarginalUtility = Double.parseDouble(lowerMUtility);
		if ((load < lowerBound) && load>0) {
			tmpScore += lowerMarginalUtility/load * (minimalDuration - duration);
		}
		
		// disutility of agent interaction overarousal
		String thresholdOverArousal = capacities.getUpperBound();
		double upperBound = Double.parseDouble(thresholdOverArousal);
		String upperMUtility = capacities.getUpperMarginalUtility();
		double upperMarginalUtility = Double.parseDouble(upperMUtility);
		if ((load > upperBound)) {
			tmpScore += upperMarginalUtility*load * (minimalDuration - duration);
		}
		
		return tmpScore;
	}
}
