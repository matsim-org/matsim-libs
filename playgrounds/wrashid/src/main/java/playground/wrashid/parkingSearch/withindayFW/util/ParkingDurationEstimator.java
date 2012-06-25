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

package playground.wrashid.parkingSearch.withindayFW.util;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

public class ParkingDurationEstimator {

	public static Double estimateParkingDurationLastParkingOfDay(double arrivalTime, double firstCarDepartureTimeOfDay) {
		return GeneralLib.getIntervalDuration(arrivalTime, firstCarDepartureTimeOfDay);
	}

	// TODO: instead of this, we could also only consider the actual activity
	// durations (without the walk legs/parking activity, to be consistent with
	// the figures used elsewhere 
	public static double estimateParkingDurationDuringDay(double currentTime, List<PlanElement> planElements,
			int currentCarLegPlanElementIndex) {
		double estimatedActduration = 0;
		for (int i = currentCarLegPlanElementIndex + 1; i < planElements.size(); i++) {

			if (planElements.get(i) instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) planElements.get(i);
				double endTime = act.getEndTime();
				double maximumDuration = act.getMaximumDuration();
				if (endTime != Double.NEGATIVE_INFINITY) {
					estimatedActduration += GeneralLib.getIntervalDuration(currentTime, endTime);
				} else {
					estimatedActduration += maximumDuration;
				}
			} else {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.car)) {
					break;
				}

				ActivityImpl prevAct = (ActivityImpl) planElements.get(i - 1);
				ActivityImpl nextAct = (ActivityImpl) planElements.get(i + 1);
				double distance = GeneralLib.getDistance(prevAct.getCoord(), nextAct.getCoord());
				if (leg.getMode().equalsIgnoreCase("walk") || leg.getMode().equalsIgnoreCase("transit_walk")) {
					estimatedActduration += GeneralLib.getWalkingTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("bike")) {
					estimatedActduration += GeneralLib.getBikeTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("pt")) {
					estimatedActduration += GeneralLib.getPtTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("ride")) {
					// as ride should disappear anyway, this the closest
					// estimation,
					// which must not be correct for the algorithm to work.
					estimatedActduration += GeneralLib.getPtTravelDuration(distance);
				} else {
					DebugLib.stopSystemAndReportInconsistency("unhandled mode:" + leg.getMode());
				}
			}

		}
		return estimatedActduration;

	}

}
