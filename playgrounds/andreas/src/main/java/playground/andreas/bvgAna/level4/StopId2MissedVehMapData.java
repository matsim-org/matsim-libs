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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * Collects number of missed vehicles and accumulated delay for a stop
 * 
 * @author aneumann
 *
 */
public class StopId2MissedVehMapData {
	
	private final Logger log = Logger.getLogger(StopId2MissedVehMapData.class);
	private final Level logLevel = Level.DEBUG;
	
	private final Id stopId;	
	private List<Integer> missedVehList = new ArrayList<Integer>();
	private List<Double> delays = new ArrayList<Double>();
	
	public StopId2MissedVehMapData(Id stopId){
		this.log.setLevel(this.logLevel);
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
	
	/**
	 * Collects data for a aggregated overview of all collected data
	 * 
	 * @return Formatted string containing the data
	 */
	public String getFullStatistics(){
		StringBuffer str = new StringBuffer();
		
		str.append(this.stopId + "; ");
		
		int nVehTooEarly = 0;
		int nVehAsPlanned = 0;
		int nVehTooLate = 0;

		int nAgentsInVehTooEarly = 0;
		int nAgentsInVehAsPlanned = 0;
		int nAgentsInVehTooLate = 0;
		
		// Statistics for agents missed a vehicles or got one too early
		for (Integer entry : this.missedVehList) {
			if(entry.intValue() < 0){
				nVehTooEarly += entry.intValue();
				nAgentsInVehTooEarly++;
			} else if(entry.intValue() == 0){
				nVehAsPlanned += entry.intValue();
				nAgentsInVehAsPlanned++;
			} else if(entry.intValue() > 0){
				nVehTooLate += entry.intValue();
				nAgentsInVehTooLate++;
			} else {
				this.log.info("Shouldn't be here");
			}
		}
		
		str.append(nVehTooEarly + "; ");
		str.append(nVehAsPlanned + "; ");
		str.append(nVehTooLate + "; ");
		str.append(nAgentsInVehTooEarly + "; ");
		str.append(nAgentsInVehAsPlanned + "; ");
		str.append(nAgentsInVehTooLate + "; ");
	
		
		double timeTooEarly = 0.0;
		double timeAsPlanned = 0.0;
		double timeTooLate = 0.0;
		
		int nAgentsTooEarly = 0;
		int nAgentsAsPlanned = 0;
		int nAgentsTooLate = 0;
		
		// Same statistics, not counting vehicles but time
		for (Double delay : this.delays) {
			if(delay.doubleValue() < 0.0){
				timeTooEarly += delay.doubleValue();
				nAgentsTooEarly++;
			} else if(delay.doubleValue() == 0.0){
				timeAsPlanned += delay.doubleValue();
				nAgentsAsPlanned++;
			} else if(delay.doubleValue() > 0.0){
				timeTooLate += delay.doubleValue();
				nAgentsTooLate++;
			} else {
				this.log.info("Shouldn't be here");
			}
		}
		
		str.append(timeTooEarly + "; ");
		str.append(timeAsPlanned + "; ");
		str.append(timeTooLate + "; ");
		str.append(nAgentsTooEarly + "; ");
		str.append(nAgentsAsPlanned + "; ");
		str.append(nAgentsTooLate + "; ");		
		
		return str.toString();
	}
	
	@Override
	public String toString() {
		return "Stop " + this.stopId + " missed vehicles " + this.getNumberOfMissedVehicles() + " with an average delay of " + this.getAverageAdditionalDelay();
	}
}