/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data.analysis;

import java.util.HashSet;
import java.util.Set;

import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectory;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoryHandler;


/**
 * @author mrieser
 */
public class CalculateTravelTimeMatrixFromVehTrajectories implements VehicleTrajectoryHandler {

	private final DynamicTravelTimeMatrix matrix;
	private final Set<String> zoneIds = new HashSet<String>();

	public CalculateTravelTimeMatrixFromVehTrajectories(final DynamicTravelTimeMatrix matrix) {
		this.matrix = matrix;
	}

	@Override
	public void handleVehicleTrajectory(final VehicleTrajectory trajectory) {
		this.zoneIds.add(trajectory.getOrigZone());
		this.zoneIds.add(trajectory.getDestZone());
		this.matrix.addTravelTime(trajectory.getStartTime(), trajectory.getTravelTime(), trajectory.getOrigZone(), trajectory.getDestZone());
	}
	
	public Set<String> getZoneIds() {
		return this.zoneIds;
	}

}
