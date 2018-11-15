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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams.LogRouteDesignScore;
import org.matsim.contrib.minibus.genericUtils.TerminusStopFinder;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.vividsolutions.jts.geom.Polygon;

/**
 * Calculate penalty score for the area between all TransitStopFacilities (of
 * the TransitRoute of the PPlan) divided by the beeline distance between the
 * two terminus stops of the stopsToBeServed of the PPlan.
 * 
 * TransitRoutes which operate in different roads per direction have a bigger
 * area between all TransitStopFacilities, so this can be useful to encourage
 * TransitRoutes going back and forth on the same road instead of more circular
 * routes.
 * 
 * @author gleich
 *
 */
class AreaBtwStopsVsTerminiBeelinePenalty implements RouteDesignScoringFunction {

	final static Logger log = Logger.getLogger(AreaBtwStopsVsTerminiBeelinePenalty.class);
	private final RouteDesignScoreParams params;

	public AreaBtwStopsVsTerminiBeelinePenalty(RouteDesignScoreParams params) {
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

		List<Coord> coords = new ArrayList<>();
		for (TransitStopFacility stop : stopListToEvaluate) {
			coords.add(stop.getCoord());
		}

		double area = 0;

		if (coords.size() < 3) {
			// not enough coords to calculate an area, no scoring possible
			return 0;
		} else {
			try {
				Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
				area = polygon.getArea();
			} catch (IllegalArgumentException e) {
				log.warn(e.getMessage());
			}
		}

		double score = area / beelineLength - params.getValueToStartScoring();
		if (score > 0) {
			score = params.getCostFactor() * score;
		} else {
			// return 0 if score better than valueToStartScoring; it is a penalty, not a
			// subsidy
			score = 0;
		}

		if (params.getLogScore().equals(LogRouteDesignScore.onlyNonZeroScore) && score != 0) {
			log.info("Transit Route " + route.getId() + " scored " + score + " (area " + area + "; beeline "
					+ beelineLength);
		}
		return score;
	}

}
