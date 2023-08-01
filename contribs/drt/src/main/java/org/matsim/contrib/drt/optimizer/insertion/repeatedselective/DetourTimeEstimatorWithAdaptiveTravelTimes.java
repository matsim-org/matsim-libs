/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.repeatedselective;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.zone.skims.AdaptiveTravelTimeMatrix;
import org.matsim.core.router.util.TravelTime;

import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;

/**
 * @author steffenaxer
 */
public class DetourTimeEstimatorWithAdaptiveTravelTimes implements DetourTimeEstimator {

	private final TravelTime travelTime;
	private final AdaptiveTravelTimeMatrix adaptiveTravelTimeMatrix;
	private final double speedFactor;

	DetourTimeEstimatorWithAdaptiveTravelTimes(double speedFactor, AdaptiveTravelTimeMatrix adaptiveTravelTimeMatrix,
											   TravelTime travelTime) {
		this.speedFactor = speedFactor;
		this.adaptiveTravelTimeMatrix = adaptiveTravelTimeMatrix;
		this.travelTime = travelTime;
	}

	public static DetourTimeEstimatorWithAdaptiveTravelTimes create(double speedFactor, AdaptiveTravelTimeMatrix updatableTravelTimeMatrix,
																	TravelTime travelTime) {
		return new DetourTimeEstimatorWithAdaptiveTravelTimes(speedFactor, updatableTravelTimeMatrix, travelTime);
	}

	@Override
	public double estimateTime(Link from, Link to, double departureTime) {

		if (from == to) {
			return 0;
		}

		double duration = FIRST_LINK_TT;
		duration += this.getTravelTime(from.getToNode(), to.getFromNode(), departureTime + duration);
		duration += VrpPaths.getLastLinkTT(travelTime, to, departureTime + duration);
		return duration / speedFactor;
	}

	double getTravelTime(Node fromNode, Node toNode, double departureTime) {
		return this.adaptiveTravelTimeMatrix.getTravelTime(fromNode, toNode, departureTime);
	}

}
