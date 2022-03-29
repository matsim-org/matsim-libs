/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public interface DetourTimeEstimator {
	static DetourTimeEstimator createBeelineBasedEstimator(double beelineSpeed) {
		return (from, to, departureTime) -> DistanceUtils.calculateDistance(from.getToNode(), to.getToNode())
				/ beelineSpeed;
	}

	static DetourTimeEstimator createMatrixBasedEstimator(double speedFactor, TravelTimeMatrix matrix,
			TravelTime travelTime) {
		return (from, to, departureTime) -> {
			if (from == to) {
				return 0;
			}
			double duration = FIRST_LINK_TT;
			duration += matrix.getTravelTime(from.getToNode(), to.getFromNode(), departureTime + duration);
			duration += VrpPaths.getLastLinkTT(travelTime, to, departureTime + duration);
			return duration / speedFactor;
		};
	}

	double estimateTime(Link from, Link to, double departureTime);
}
