/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.lib.tools.tripReader.Trip;

import java.util.List;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class AVAssignment {

	/**
	 * Searches for an AV to serve the request.
	 *
	 * @param requestToHandle
	 * @param availableAVs
	 * @return The position of the assigned AV in the list of availableAVs
	 * 			OR -1 if no AV currently satisfies all search conditions.
	 */
	public static int assignVehicleToRequest(Trip requestToHandle, List<AutonomousVehicle> availableAVs) {
		final Coord requestStartLocation = new CoordImpl(requestToHandle.startXCoord, requestToHandle.startYCoord);

		// --- Different search approaches: ---
		//return getAbsoluteClosest(requestStartLocation, availableAVs);
		return getClosestWithinLevelOfServiceSearchRadius(requestStartLocation, availableAVs);
	}

	private static int getAbsoluteClosest(Coord requestStartLocation, List<AutonomousVehicle> availableAVs) {
		int closestVehicle = -1;
		double minDistance = Double.MAX_VALUE;

		for (int i = 0; i < availableAVs.size(); i++) {
			double distance = CoordUtils.calcDistance(requestStartLocation, availableAVs.get(i).getMyPosition());
			if (distance < minDistance) {
				minDistance = distance;
				closestVehicle = i;
			}
		}

		return closestVehicle;
	}

	private static int getClosestWithinLevelOfServiceSearchRadius(Coord requestStartLocation, List<AutonomousVehicle> availableAVs) {
		int closestVehicle = getAbsoluteClosest(requestStartLocation, availableAVs);
		double distance = CoordUtils.calcDistance(requestStartLocation, availableAVs.get(closestVehicle).getMyPosition());
		if (distance <= Constants.getSearchRadius()) {
			return closestVehicle;
		} else {
			return -1;
		}
	}

}
