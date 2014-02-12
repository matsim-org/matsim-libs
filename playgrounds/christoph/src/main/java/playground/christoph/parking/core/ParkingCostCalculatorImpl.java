/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.core;

import java.util.HashMap;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;

import playground.christoph.parking.ParkingTypes;
import playground.christoph.parking.core.interfaces.ParkingCostCalculator;

public class ParkingCostCalculatorImpl implements ParkingCostCalculator {

	private final HashMap<String, HashSet<Id>> parkingTypes;

	public ParkingCostCalculatorImpl(HashMap<String, HashSet<Id>> parkingTypes) {
		this.parkingTypes = parkingTypes;
	}
	
	@Override
	public Double getParkingCost(Id parkingFacilityId, Id vehicleId, Id driverId, double arrivalTime, double parkingDuration) {
		
		if (parkingTypes.get(ParkingTypes.STREETPARKING).contains(parkingFacilityId)) {
			return 1.0 * parkingDuration/3600;
		} else if(parkingTypes.get(ParkingTypes.GARAGEPARKING).contains(parkingFacilityId)) {
			return 2.0 * parkingDuration/3600;
		} else if (parkingTypes.get(ParkingTypes.PRIVATEINSIDEPARKING).contains(parkingFacilityId)) {
			return 0.5;
		} else if (parkingTypes.get(ParkingTypes.PRIVATEOUTSIDEPARKING).contains(parkingFacilityId)) {
			return 0.0;
		}
		
		return Double.MAX_VALUE;
	}

}