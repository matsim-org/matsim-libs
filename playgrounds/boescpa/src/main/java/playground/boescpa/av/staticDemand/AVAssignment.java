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
import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.analysis.trips.Trip;

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
        // --- Different search approaches: ---
        //return getAbsoluteClosest(new CoordImpl(requestToHandle.startXCoord, requestToHandle.startYCoord), availableAVs);
        //return getClosestWithinLevelOfServiceSearchRadius(requestToHandle, availableAVs);
        return getClosestWithinLevelOfServiceSearchRadius_RadiusReducing(requestToHandle, availableAVs);
    }

    private static int getAbsoluteClosest(Coord requestStartLocation, List<AutonomousVehicle> availableAVs) {
        int closestVehicle = -1;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < availableAVs.size(); i++) {
            double distance = CoordUtils.calcEuclideanDistance(requestStartLocation, availableAVs.get(i).getMyPosition());
            if (distance < minDistance) {
                minDistance = distance;
                closestVehicle = i;
            }
        }
        return closestVehicle;
    }

    private static int getClosestWithinLevelOfServiceSearchRadius(Trip requestToHandle, List<AutonomousVehicle> availableAVs) {
        final Coord requestStartLocation = CoordUtils.createCoord(requestToHandle.startXCoord, requestToHandle.startYCoord);
        int closestVehicle = getAbsoluteClosest(requestStartLocation, availableAVs);
        if (closestVehicle == -1) {
            return -1;
        }
        double distance = CoordUtils.calcEuclideanDistance(requestStartLocation, availableAVs.get(closestVehicle).getMyPosition());
        if (distance <= Constants.getSearchRadiusLevelOfService()) {
            return closestVehicle;
        } else {
            return -1;
        }
    }

    private static int getClosestWithinLevelOfServiceSearchRadius_RadiusReducing(Trip requestToHandle, List<AutonomousVehicle> availableAVs) {
        final Coord requestStartLocation = CoordUtils.createCoord(requestToHandle.startXCoord, requestToHandle.startYCoord);
        int closestVehicle = getAbsoluteClosest(requestStartLocation, availableAVs);
        if (closestVehicle == -1) {
            return -1;
        }
        double distanceVehicles = CoordUtils.calcEuclideanDistance(requestStartLocation, availableAVs.get(closestVehicle).getMyPosition());
        int searchTime = (int)requestToHandle.startTime + Constants.LEVEL_OF_SERVICE - StaticAVSim.getTime();
        if (distanceVehicles <= Constants.getSearchRadius(searchTime)) {
            return closestVehicle;
        } else {
            return -1;
        }
    }
}
