/* *********************************************************************** *
 * project: org.matsim.*
 * sdfasfwaeg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

public class LegScoringFunction4PC extends LegScoringFunction {

	protected double travTimeAttrCar/* [h] */= 0d, travTimeAttrPt/* [h] */= 0d,
			travTimeAttrWalk/* [h] */= 0d;

	public LegScoringFunction4PC(Plan plan,
			CharyparNagelScoringParameters params) {
		super(params);
	}

	public double getTravTimeAttrPt() {
		return travTimeAttrPt;
	}

	public double getTravTimeAttrWalk() {
		return travTimeAttrWalk;
	}

	public double getTravTimeAttrCar() {
		return travTimeAttrCar;
	}

	public void reset() {
		super.reset();
		travTimeAttrCar = 0d;
		travTimeAttrPt = 0d;
		travTimeAttrWalk = 0d;
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			Leg leg) {
		double tmpScore = 0d;
		double travelTime = arrivalTime - departureTime; // traveltime in
		// seconds

		/*
		 * we only as for the route when we have to calculate a distance cost,
		 * because route.getDist() may calculate the distance if not yet
		 * available, which is quite an expensive operation
		 */
		double dist = 0.0; // distance in meters

		if (TransportMode.car.equals(leg.getMode())) {
			if (params.marginalUtilityOfDistanceCar_m != 0.0) {
				RouteWRefs route = (RouteWRefs) leg.getRoute();
				dist = route.getDistance();
				/*
				 * TODO the route-distance does not contain the length of the
				 * first or last link of the route, because the route doesn't
				 * know those. Should be fixed somehow, but how? MR, jan07
				 */
				/*
				 * TODO in the case of within-day replanning, we cannot be sure
				 * that the distance in the leg is the actual distance driven by
				 * the agent.
				 */
			}
			tmpScore += travelTime * params.marginalUtilityOfTraveling_s
					+ params.marginalUtilityOfDistanceCar_m * dist;
			// traveling attr
			travTimeAttrCar += travelTime / 3600d;
		} else if (TransportMode.pt.equals(leg.getMode())) {
			if (params.marginalUtilityOfDistancePt_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * params.marginalUtilityOfTravelingPT_s
					+ params.marginalUtilityOfDistancePt_m * dist;
			// travelingPt attr
			travTimeAttrPt += travelTime / 3600d;
		} else if (TransportMode.walk.equals(leg.getMode())
				|| TransportMode.transit_walk.equals(leg.getMode())) {
			if (params.marginalUtilityOfDistanceWalk_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			tmpScore += travelTime * params.marginalUtilityOfTravelingWalk_s
					+ params.marginalUtilityOfDistanceWalk_m * dist;
			// travelingWalk attr
			travTimeAttrWalk += travelTime / 3600d;
		} else {// other mode?
			if (params.marginalUtilityOfDistanceCar_m != 0.0) {
				dist = leg.getRoute().getDistance();
			}
			// use the same values as for "car"
			tmpScore += travelTime * params.marginalUtilityOfTraveling_s
					+ params.marginalUtilityOfDistanceCar_m * dist;
			// traveling attr
			travTimeAttrCar += travelTime / 3600d;
		}

		return tmpScore;
	}
}
