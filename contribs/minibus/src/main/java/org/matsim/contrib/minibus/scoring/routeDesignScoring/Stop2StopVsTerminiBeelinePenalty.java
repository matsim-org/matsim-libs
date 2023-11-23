/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.scoring.routeDesignScoring;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams.LogRouteDesignScore;
import org.matsim.contrib.minibus.genericUtils.TerminusStopFinder;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Calculate penalty score for circuitous TransitRoutes (TransitRoute of the
 * PPlan) measured as sum of all beeline distances of consecutive
 * StopsToBeServed TransitStops divided by the beeline distance between the two
 * terminus stops of the stopsToBeServed of the PPlan.
 * 
 * @author gleich
 *
 */
class Stop2StopVsTerminiBeelinePenalty implements RouteDesignScoringFunction {

	private static final Logger log = LogManager.getLogger(Stop2StopVsTerminiBeelinePenalty.class);
	private final RouteDesignScoreParams params;

	public Stop2StopVsTerminiBeelinePenalty(RouteDesignScoreParams params) {
		this.params = params;
	}

	@Override
	public double getScore(PPlan pPlan, TransitRoute route) {
		TransitStopFacility startStop = pPlan.getStopsToBeServed().get(0);
		TransitStopFacility endStop = pPlan.getStopsToBeServed()
				.get(TerminusStopFinder.findSecondTerminusStop(pPlan.getStopsToBeServed()));
		double beelineLength = CoordUtils.calcEuclideanDistance(startStop.getCoord(), endStop.getCoord());

		List<TransitStopFacility> stopListToEvaluate = new ArrayList<>();
		switch (params.getStopListToEvaluate()) {
		case transitRouteAllStops:
			for (TransitRouteStop stop : route.getStops()) {
				stopListToEvaluate.add(stop.getStopFacility());
			}
			break;
		case pPlanStopsToBeServed:
			stopListToEvaluate = pPlan.getStopsToBeServed();
			break;
		default:
			log.error("Unknown stopListToEvaluate parameter :" + params.getStopListToEvaluate());
			new RuntimeException();
		}

		double lengthStop2Stop = 0.0;
		TransitStopFacility previousStop = stopListToEvaluate.get(0);

		for (int i = 0; i < stopListToEvaluate.size(); i++) {
			TransitStopFacility currentStop = stopListToEvaluate.get(i);
			lengthStop2Stop = lengthStop2Stop
					+ CoordUtils.calcEuclideanDistance(previousStop.getCoord(), currentStop.getCoord());
			previousStop = currentStop;
		}
		// add leg from last to first stop
		lengthStop2Stop = lengthStop2Stop
				+ CoordUtils.calcEuclideanDistance(previousStop.getCoord(), stopListToEvaluate.get(0).getCoord());

		double score = lengthStop2Stop / beelineLength - params.getValueToStartScoring();
		if (score > 0) {
			score = params.getCostFactor() * score;
		} else {
			// return 0 if score better than valueToStartScoring; it is a penalty, not a
			// subsidy
			score = 0;
		}
		
		if (params.getLogScore().equals(LogRouteDesignScore.onlyNonZeroScore) && score != 0) {
			log.info("Transit Route " + route.getId() + " scored " + score + " (length stop2stop " + lengthStop2Stop
					+ "; beeline " + beelineLength);
		}
		return score;
	}

}
