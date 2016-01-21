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

package playground.wrashid.parkingSearch.withindayFW.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Pair;

import org.matsim.core.controler.MatsimServices;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;

public abstract class ParkingAnalysisHandler {

	protected MatsimServices controler;
	
	public void updateParkingOccupancyStatistics(ParkingOccupancyStats parkingOccupancy, IntegerValueHashMap<Id> facilityCapacities, int iteration){
		parkingOccupancy.writeOutParkingOccupanciesTxt(controler,facilityCapacities, iteration);
		parkingOccupancy.writeOutParkingOccupancySumPng(controler, iteration);
	}
	
	public abstract void processParkingWalkTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog, int iteration);
	
	public abstract void processParkingSearchTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimeLog, int iteration);
	public abstract void processParkingCost(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog, int iteration);

	public abstract void printShareOfCarUsers(); 
}
