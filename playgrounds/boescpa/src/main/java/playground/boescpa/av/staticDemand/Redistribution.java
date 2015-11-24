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

import playground.boescpa.analysis.trips.tripReader.Trip;

import java.util.List;
import java.util.Map;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public interface Redistribution {

	List<AutonomousVehicle> getRedistributingVehicles(List<AutonomousVehicle> availableVehicles, List<Trip> openRequests);

	void updateCurrentForces(Map<AutonomousVehicle, ForceModel.Force> availableVehicles, List<Trip> openRequests);

	void moveRedistributingVehicle(AutonomousVehicle vehicleToRedistribute, ForceModel.Force force);

}
