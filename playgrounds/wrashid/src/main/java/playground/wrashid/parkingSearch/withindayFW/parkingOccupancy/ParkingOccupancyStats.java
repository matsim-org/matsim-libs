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

package playground.wrashid.parkingSearch.withindayFW.parkingOccupancy;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;

import org.matsim.core.controler.MatsimServices;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;

public class ParkingOccupancyStats {

	private int numberOfMaximumParkingCapacitConstraintViolations=0;
	public HashMap<Id, ParkingOccupancyBins> parkingOccupancies = new HashMap<Id, ParkingOccupancyBins>();
	
	public void updateParkingOccupancy(Id parkingFacilityId, Double arrivalTime, Double departureTime, int parkingCapacity) {
		if (!parkingOccupancies.containsKey(parkingFacilityId)) {
			parkingOccupancies.put(parkingFacilityId, new ParkingOccupancyBins());
		}

		ParkingOccupancyBins parkingOccupancyBins = parkingOccupancies.get(parkingFacilityId);
		parkingOccupancyBins.inrementParkingOccupancy(arrivalTime, departureTime);
		

		
		if (parkingOccupancyBins.isMaximumCapacityConstraintViolated(parkingCapacity)){
			DebugLib.emptyFunctionForSettingBreakPoint();
			setNumberOfMaximumParkingCapacitConstraintViolations(getNumberOfMaximumParkingCapacitConstraintViolations() + 1);
			//System.out.println(numberOfMaximumParkingCapacitConstraintViolations);
		}
	}
	
	
	public void writeOutParkingOccupanciesTxt(MatsimServices controler, IntegerValueHashMap<Id> facilityCapacities, int iteration) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancy.txt");

		ArrayList<String> list = new ArrayList<String>();

		// create header line
		StringBuffer row = new StringBuffer("name\tcapacity");
		
		for (int i = 0; i < 96; i++) {
			row.append("\t");
			row.append("bin-"+i);
		}
		
		list.add(row.toString());
		
		// content
		for (Id parkingFacilityId : parkingOccupancies.keySet()) {

			ParkingOccupancyBins parkingOccupancyBins = parkingOccupancies.get(parkingFacilityId);
			row = new StringBuffer(parkingFacilityId.toString());

			row.append("\t");
			row.append(facilityCapacities.get(parkingFacilityId));
			
			for (int i = 0; i < 96; i++) {
				row.append("\t");
				row.append(parkingOccupancyBins.getOccupancy(i * 900));
			}

			list.add(row.toString());
		}

		GeneralLib.writeList(list, iterationFilename);
	}
	
	public void writeOutParkingOccupancySumPng(MatsimServices controler, int iteration) {
		String fileName = controler.getControlerIO().getIterationFilename(iteration,
		"parkingOccupancyAllParking.png");
		
		double matrix[][] = new double[96][1];
		String title="parked vehicles at all parkings in the simulation";
		String xLabel="time";
		String yLabel="number of vehicles parked";
		String[] seriesLabels={"all parking occupancy"};
		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i *0.25;
		}
		
		for (Id parkingFacilityId : parkingOccupancies.keySet()) {

			int[] occupancy = parkingOccupancies.get(parkingFacilityId).getOccupancy();
			
			for (int i = 0; i < 96; i++) {
				matrix[i][0] += occupancy[i];
			}
		}
		
		GeneralLib.writeGraphic(fileName, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}


	public int getNumberOfMaximumParkingCapacitConstraintViolations() {
		return numberOfMaximumParkingCapacitConstraintViolations;
	}


	public void setNumberOfMaximumParkingCapacitConstraintViolations(int numberOfMaximumParkingCapacitConstraintViolations) {
		this.numberOfMaximumParkingCapacitConstraintViolations = numberOfMaximumParkingCapacitConstraintViolations;
	}
}
