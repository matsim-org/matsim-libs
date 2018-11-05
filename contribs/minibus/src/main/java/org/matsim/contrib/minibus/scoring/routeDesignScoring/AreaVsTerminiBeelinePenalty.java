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
 * @author gregor
 *
 */
class AreaVsTerminiBeelinePenalty implements RouteDesignScoringFunction {

	final static Logger log = Logger.getLogger(AreaVsTerminiBeelinePenalty.class);
	private final RouteDesignScoreParams params;

	public AreaVsTerminiBeelinePenalty(RouteDesignScoreParams params) {
		this.params = params;
	}

	@Override
	public double getScore(PPlan pPlan, TransitRoute route) {
		TransitStopFacility startStop = pPlan.getStopsToBeServed().get(0);
		TransitStopFacility endStop = pPlan.getStopsToBeServed()
				.get(TerminusStopFinder.findStopIndexWithLargestDistance(pPlan.getStopsToBeServed()));
		double beelineLength = CoordUtils.calcEuclideanDistance(startStop.getCoord(), endStop.getCoord());

		List<Coord> coords = new ArrayList<>();
		for (TransitRouteStop stop : route.getStops()) {
			coords.add(stop.getStopFacility().getCoord());
		}

		double area = 0;

		try {
			Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
			area = polygon.getArea();
		} catch (IllegalArgumentException e) {
			log.warn(e.getMessage());
		}

		return params.getCostFactor() * ((area / beelineLength) - params.getValueToStartScoring());
	}

}
