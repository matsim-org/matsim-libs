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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams.LogRouteDesignScore;
import org.matsim.contrib.minibus.hook.PPlan;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Calculate penalty score for circuitous TransitRoutes (TransitRoute of the
 * PPlan) which overlap themselves, i.e. the same stop is served several times
 * by the same TransitRoute. Score is calculated as number of stops on the route
 * divided by the number of unique stop facilities served.
 * 
 * @author gleich
 *
 */
class StopServedMultipleTimesPenalty implements RouteDesignScoringFunction {

	private static final Logger log = LogManager.getLogger(StopServedMultipleTimesPenalty.class);
	private final RouteDesignScoreParams params;

	public StopServedMultipleTimesPenalty(RouteDesignScoreParams params) {
		this.params = params;
	}

	@Override
	public double getScore(PPlan pPlan, TransitRoute route) {
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

		Set<Id<TransitStopFacility>> stopIdServed = new HashSet<>();

		for (int i = 0; i < stopListToEvaluate.size(); i++) {
			Id<TransitStopFacility> currentStop = stopListToEvaluate.get(i).getId();
			if (! stopIdServed.contains(currentStop)) {
				stopIdServed.add(currentStop);
			}
		}

		double score = ((double) stopListToEvaluate.size()) / stopIdServed.size() - params.getValueToStartScoring();
		if (score > 0) {
			score = params.getCostFactor() * score;
		} else {
			// return 0 if score better than valueToStartScoring; it is a penalty, not a
			// subsidy
			score = 0;
		}

		if (params.getLogScore().equals(LogRouteDesignScore.onlyNonZeroScore) && score != 0) {
			log.info("Transit Route " + route.getId() + " scored " + score + " (total " + stopListToEvaluate.size()
					+ " stops served; unique TransitStopFacilities " + stopIdServed.size());
		}
		return score;
	}

}
