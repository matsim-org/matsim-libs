/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.ikaddoura.moneyTravelDisutility.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
* Stores vehicle-specific monetary amounts for each time bin.
*  
* @author ikaddoura
*/

public class TimeBin {

	private int timeBinNr;
	
	private final Map<Id<Vehicle>, List<Double>> vehicleId2amounts;
	private double averageAmount = 0.;

	private final Map<VehicleType, Double> vehicleType2avgAmount = new HashMap<>(); // TODO
	
	public TimeBin(int timeBinNr) {
		this.timeBinNr = timeBinNr;
		this.vehicleId2amounts = new HashMap<>();
	}

	public double getTimeBinNr() {
		return timeBinNr;
	}

	public Map<Id<Vehicle>, List<Double>> getVehicleId2amounts() {
		return vehicleId2amounts;
	}

	public double getAverageAmount() {
		return averageAmount;
	}

	public Map<VehicleType, Double> getVehicleType2avgAmount() {
		throw new RuntimeException("ToDo! Not yet implemented. Aborting...");
//		return vehicleType2avgAmount;
	}

	public void process() {
		computeAverageAmountPerVehicle();
		computeAverageAmountPerVehicleType();
	}

	private void computeAverageAmountPerVehicle() {
		double sum = 0.;
		int counter = 0;
		for (Id<Vehicle> vehicleId : vehicleId2amounts.keySet()) {
			for (Double amount : vehicleId2amounts.get(vehicleId)) {
				sum += amount;
				counter++;
			}
		}
		
		this.averageAmount = sum / counter;
	}

	private void computeAverageAmountPerVehicleType() {
		// TODO Auto-generated method stub
	}

}

