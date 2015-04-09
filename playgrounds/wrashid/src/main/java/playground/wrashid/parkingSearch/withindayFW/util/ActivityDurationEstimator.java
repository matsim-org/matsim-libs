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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;


public class ActivityDurationEstimator {

	public static Double estimateActivityDurationLastParkingOfDay(double arrivalTime, double firstCarDepartureTimeOfDay) {
		return GeneralLib.getIntervalDuration(arrivalTime, firstCarDepartureTimeOfDay);
	}


	public static double estimateActivityDurationParkingDuringDay(double currentTime, List<PlanElement> planElements,
			int currentCarLegPlanElementIndex, PlansCalcRouteConfigGroup pcrConfig) {
		double estimatedActduration = 0;
		
		int indexOfFirstActivity = currentCarLegPlanElementIndex + 3;
		for (int i = indexOfFirstActivity; i < planElements.size(); i++) {

			if (planElements.get(i) instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) planElements.get(i);
				double endTime = act.getEndTime();
				double maximumDuration = act.getMaximumDuration();
				if (endTime != Double.NEGATIVE_INFINITY) {
					estimatedActduration += GeneralLib.getIntervalDuration(currentTime+estimatedActduration, endTime);
				} else {
					estimatedActduration += maximumDuration;
				}
			} else {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.walk) && ((Activity) planElements.get(i+1)).getType().equalsIgnoreCase("parking")) {
					break;
				}

				ActivityImpl prevAct = (ActivityImpl) planElements.get(i - 1);
				ActivityImpl nextAct = (ActivityImpl) planElements.get(i + 1);
				double distance = GeneralLib.getDistance(prevAct.getCoord(), nextAct.getCoord());
				if (leg.getMode().equalsIgnoreCase("walk") || leg.getMode().equalsIgnoreCase("transit_walk")) {
					estimatedActduration += GeneralLib.getWalkingTravelDuration(distance, pcrConfig);
				} else if (leg.getMode().equalsIgnoreCase("bike")) {
					estimatedActduration += GeneralLib.getBikeTravelDuration(distance, pcrConfig);
				} else if (leg.getMode().equalsIgnoreCase("pt")) {
					estimatedActduration += GeneralLib.getPtTravelDuration(distance, pcrConfig);
				} else if (leg.getMode().equalsIgnoreCase("ride")) {
					// as ride should disappear anyway, this the closest
					// estimation,
					// which must not be correct for the algorithm to work.
					estimatedActduration += GeneralLib.getPtTravelDuration(distance, pcrConfig);
				} else {
					DebugLib.stopSystemAndReportInconsistency("unhandled mode:" + leg.getMode());
				}
			}

		}
		return estimatedActduration;

	}

}
