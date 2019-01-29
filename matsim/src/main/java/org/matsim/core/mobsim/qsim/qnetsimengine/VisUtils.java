/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.mobsim.framework.PassengerAgent;

/**
 * @author nagel
 *
 */
final class VisUtils {
	private VisUtils() {} // only for static methods; do not instantiate

	/**
	 * Returns all the people sitting in this vehicle.
	 *
	 * @param vehicle
	 * @return All the people in this vehicle. If there is more than one, the first entry is the driver.
	 */
	final static List<Identifiable<?>> getPeopleInVehicle(QVehicle vehicle) {
		ArrayList<Identifiable<?>> people = new ArrayList<>();
		people.add(vehicle.getDriver());
		for ( PassengerAgent passenger : vehicle.getPassengers() ) {
			people.add(passenger) ;
		}
		return people;
	}

	public final static Integer guessLane(QVehicle veh, int numberOfLanes){
		Integer tmpLane;
		try {
			tmpLane = Integer.parseInt(veh.getId().toString()) ;
		} catch ( NumberFormatException ee ) {
			tmpLane = veh.getId().hashCode() ;
			if (tmpLane < 0 ){
				tmpLane = -tmpLane;
			}
		}
		int lane = 1 + (tmpLane % numberOfLanes);
		return lane;
	}

	public final static double calcSpeedValueBetweenZeroAndOne(QVehicle veh, double inverseSimulatedFlowCapacity, double now, double freespeed){
		int cmp = (int) (veh.getEarliestLinkExitTime() + inverseSimulatedFlowCapacity + 2.0);
		// "inverseSimulatedFlowCapacity" is there to keep vehicles green that only wait for capacity (i.e. have no vehicle
		// ahead). Especially important with small samples sizes.  This is debatable :-).  kai, jan'11
	
		double speed = (now > cmp ? 0.0 : 1.0);
		return speed;
	}

}
