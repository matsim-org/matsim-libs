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

import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.av.staticDemand.ForceModel.Force;
import playground.boescpa.analysis.trips.Trip;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class ForcefieldRedistribution implements Redistribution {
	private final static ForceModel forceModel = new ForceModel();

	@Override
	public List<AutonomousVehicle> getRedistributingVehicles(List<AutonomousVehicle> availableVehicles, List<Trip> openRequests) {
		List<Force> resultingForcesAvailableVehicles = getResultingForces(availableVehicles, openRequests);
		// return the first x% of the available cars
		List<AutonomousVehicle> vehiclesToRedistribute = new ArrayList<>();
		for (int i = 0; i < Constants.SHARE_OF_FREE_AV_TO_REDISTRIBUTE*availableVehicles.size(); i++) {
			vehiclesToRedistribute.add(availableVehicles.get(resultingForcesAvailableVehicles.get(i).identifier));
		}
		return vehiclesToRedistribute;
	}

	private List<Force> getResultingForces(List<AutonomousVehicle> availableVehicles, List<Trip> openRequests) {
		// get all resulting forces for all available vehicles:
		List<Force> resultingForcesAvailableVehicles = new ArrayList<>();
		for (int i = 0; i < availableVehicles.size(); i++) {
			Force resForce = forceModel.resultingForce(availableVehicles.get(i).getMyPosition(), openRequests, availableVehicles);
			resForce.identifier = i;
			resultingForcesAvailableVehicles.add(resForce);
		}
		// sort resulting forces from greatest to smallest:
		Collections.sort(resultingForcesAvailableVehicles, new Comparator<Force>() {
            @Override
            public int compare(Force o1, Force o2) {
                return (int) Math.round(100 * (o2.getStrength() - o1.getStrength()));
            }
        });
		return resultingForcesAvailableVehicles;
	}

	@Override
	public void updateCurrentForces(Map<AutonomousVehicle, Force> availableVehicles, List<Trip> openRequests) {
		for (AutonomousVehicle vehicle : availableVehicles.keySet()) {
			Force resForce = forceModel.resultingForce(vehicle.getMyPosition(), openRequests, availableVehicles.keySet());
			availableVehicles.put(vehicle, resForce);
		}
	}

	@Override
	public void moveRedistributingVehicle(AutonomousVehicle vehicleToRedistribute, Force force) {
		double factor = 0;
		if (force.getStrength() != 0) {
			factor = Constants.getRedistributingMoveRadius() / force.getStrength();
		}
		double newXCoord = vehicleToRedistribute.getMyPosition().getX() + (force.xPart * factor);
		double newYCoord = vehicleToRedistribute.getMyPosition().getY() + (force.yPart * factor);
		vehicleToRedistribute.moveTo(CoordUtils.createCoord(newXCoord, newYCoord));
	}
}
