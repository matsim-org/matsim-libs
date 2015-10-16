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

import playground.boescpa.lib.tools.tripReader.Trip;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class AttractionRedistribution implements Redistribution {

	private static final ForceModel forceModel = new ForceModel();
	private static final ForcefieldRedistribution forcefieldRedistribution = new ForcefieldRedistribution();

	@Override
	public List<AutonomousVehicle> getRedistributingVehicles(List<AutonomousVehicle> availableVehicles, List<Trip> openRequests) {
		List<AutonomousVehicle> availableVehiclesSorted = new ArrayList<>();
		availableVehiclesSorted.addAll(availableVehicles);
		Collections.sort(availableVehiclesSorted, new Comparator<AutonomousVehicle>() {
            @Override
            public int compare(AutonomousVehicle o1, AutonomousVehicle o2) {
                return o1.getLastArrivalTime() - o2.getLastArrivalTime();
            }
        });
		// return the longest not moved x% of the available cars IF they were not moved within the last 2xConstants.REDISTRIBUTIONINTERVAL.
		List<AutonomousVehicle> vehiclesToRedistribute = new ArrayList<>();
		for (int i = 0; i < Constants.SHARE_OF_FREE_AV_TO_REDISTRIBUTE*availableVehiclesSorted.size(); i++) {
			if (StaticAVSim.getTime() - availableVehiclesSorted.get(i).getLastArrivalTime() >
					Constants.HOW_MANY_TIMES_REDISTRIBUTIONINTERVAL_WAITING_FOR_REDISTRIBUTION * Constants.REDISTRIBUTIONINTERVAL) {
				vehiclesToRedistribute.add(availableVehiclesSorted.get(i));
			} else {
				break;
			}
		}
		return vehiclesToRedistribute;
	}

	@Override
	public void updateCurrentForces(Map<AutonomousVehicle, ForceModel.Force> availableVehicles, List<Trip> openRequests) {
		for (AutonomousVehicle vehicle : availableVehicles.keySet()) {
			ForceModel.Force resForce = forceModel.resultingForce(vehicle.getMyPosition(), openRequests, null);
			availableVehicles.put(vehicle, resForce);
		}
	}

	@Override
	public void moveRedistributingVehicle(AutonomousVehicle vehicleToRedistribute, ForceModel.Force force) {
		forcefieldRedistribution.moveRedistributingVehicle(vehicleToRedistribute, force);
	}
}
