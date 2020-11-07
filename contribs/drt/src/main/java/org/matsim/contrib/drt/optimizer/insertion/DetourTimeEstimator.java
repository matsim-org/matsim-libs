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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;

/**
 * @author michalm
 */
public interface DetourTimeEstimator {
	static DetourTimeEstimator createNodeToNodeBeelineTimeEstimator(double beelineSpeed) {
		return (from, to) -> DistanceUtils.calculateDistance(from.getToNode(), to.getToNode()) / beelineSpeed;
	}

	static DetourTimeEstimator createFreeSpeedZonalTimeEstimator(double factor, DvrpTravelTimeMatrix matrix) {
		return (from, to) -> factor * matrix.getFreeSpeedTravelTime(from.getToNode(), to.getToNode());
	}

	double estimateTime(Link from, Link to);
}
