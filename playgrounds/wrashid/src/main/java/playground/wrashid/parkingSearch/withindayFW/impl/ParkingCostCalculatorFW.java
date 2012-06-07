/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.impl;

import java.util.HashMap;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;

import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;

public class ParkingCostCalculatorFW implements ParkingCostCalculator {

	private final HashMap<String, HashSet<Id>> parkingTypes;

	public ParkingCostCalculatorFW(HashMap<String, HashSet<Id>> parkingTypes) {
		this.parkingTypes = parkingTypes;
		
	}
	
	@Override
	public Double getParkingCost(Id parkingFacilityId, double arrivalTime, double parkingDuration) {
		
		if (parkingTypes.get("streetParking").contains(parkingFacilityId)){
			return 1.0*parkingDuration/3600;
		} else if(parkingTypes.get("garageParking").contains(parkingFacilityId)){
			return 2.0*parkingDuration/3600;
		}
		
		return null;
	}

}
