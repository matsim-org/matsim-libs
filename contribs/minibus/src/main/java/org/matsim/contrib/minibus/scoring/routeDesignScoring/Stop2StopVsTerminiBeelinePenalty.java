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

import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.genericUtils.TerminusStopFinder;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

class Stop2StopVsTerminiBeelinePenalty implements RouteDesignScoringFunction {

	private final RouteDesignScoreParams params;

	public Stop2StopVsTerminiBeelinePenalty(RouteDesignScoreParams params) {
		this.params = params;
	}

	@Override
	public double getScore(PPlan pPlan, TransitRoute route) {
		TransitStopFacility startStop = pPlan.getStopsToBeServed().get(0);
		TransitStopFacility endStop = pPlan.getStopsToBeServed()
				.get(TerminusStopFinder.findStopIndexWithLargestDistance(pPlan.getStopsToBeServed()));
		double beelineLength = CoordUtils.calcEuclideanDistance(startStop.getCoord(), endStop.getCoord());

		double lengthStop2Stop = 0.0;
		TransitStopFacility previousStop = pPlan.getStopsToBeServed().get(0);

		for (int i = 0; i < pPlan.getStopsToBeServed().size(); i++) {
			TransitStopFacility currentStop = pPlan.getStopsToBeServed().get(i);
			lengthStop2Stop = lengthStop2Stop
					+ CoordUtils.calcEuclideanDistance(previousStop.getCoord(), currentStop.getCoord());
			previousStop = currentStop;
		}
		// add leg from last to first stop
		lengthStop2Stop = lengthStop2Stop + CoordUtils.calcEuclideanDistance(previousStop.getCoord(),
				pPlan.getStopsToBeServed().get(0).getCoord());

		return params.getCostFactor() * ((lengthStop2Stop / beelineLength) - params.getValueToStartScoring());
	}

}
