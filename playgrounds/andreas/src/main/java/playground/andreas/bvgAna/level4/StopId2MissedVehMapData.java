/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.level4;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * Collects number of missed vehicles and accumulated delay for a stop
 * 
 * @author aneumann
 *
 */
public class StopId2MissedVehMapData {
	
	private final Id stopId;	
	private List<Integer> missedVehList = new ArrayList<Integer>();
	private List<Double> delays = new ArrayList<Double>();
	
	public StopId2MissedVehMapData(Id stopId){
		this.stopId = stopId;
	}
	
	/**
	 * @return The number of missed vehicles. Only positive values are considered. The case an agent could get a vehicle earlier is ignored.
	 */
	public int getNumberOfMissedVehicles(){
		int numberOfMissedVehicles = 0;
		for (Integer entry : this.missedVehList) {
			if(entry.intValue() > 0){
				numberOfMissedVehicles++;
			}
		}
		return numberOfMissedVehicles;
	}
	
	/**
	 * @return Returns the average delay collected
	 */
	public double getAverageAdditionalDelay(){
		double additionalDelay = 0.0;
		int numberOfPositiveEntries = 0;
		
		for (Double delay : this.delays) {
			if(delay.doubleValue() > 0.0){
				additionalDelay += delay.doubleValue();
				numberOfPositiveEntries++;
			}			
		}
		
		return additionalDelay / numberOfPositiveEntries;
	}

	protected void addMissedVehicle(Integer numberOfMissedVehicles) {
		this.missedVehList.add(numberOfMissedVehicles);		
	}

	protected void addDelay(Double delay) {
		this.delays.add(delay);		
	}
	
	@Override
	public String toString() {
		return "Stop " + this.stopId + " missed vehicles " + this.getNumberOfMissedVehicles() + " with an average delay of " + this.getAverageAdditionalDelay();
	}
}