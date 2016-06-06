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

package playground.ikaddoura.intervalBasedCongestionPricing.data;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Stores the information which is requried during the computation of interval-based congestion tolls.
 * Contains some input variables: time bin size, how often output should be written out, internalization approach
 * 
 * @author ikaddoura
 */

public class CongestionInfo {

	// time independent values
	
	private double timeBinSize = 5 * 60.;
	private int writeOutputIteration = 10;
	private DelayInternalizationApproach internalizationApproach = DelayInternalizationApproach.MaximumDelay;
	
	public enum DelayInternalizationApproach {
        AverageDelay, LastAgentsDelay, MaximumDelay
	}
	
	private final Scenario scenario;
	
	// time interval specific values
	
	private final Map<Id<Link>, CongestionLinkInfo> linkId2info = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();
	
	private double currentTimeBinEndTime;
	
	/**
	 * Do not use the default parameters.
	 * 
	 * @param scenario
	 * @param approach
	 * @param timeBinSize
	 * @param writeOutputIteration
	 */
	public CongestionInfo(Scenario scenario, DelayInternalizationApproach approach, double timeBinSize, int writeOutputIteration) {

		this.scenario = scenario;

		this.internalizationApproach = approach;
		this.timeBinSize = timeBinSize;
		this.writeOutputIteration = writeOutputIteration;
		
		currentTimeBinEndTime = timeBinSize;
	}
	
	/**
	 * Uses the default parameters
	 * 
	 * @param scenario
	 */
	public CongestionInfo(Scenario scenario) {
		this.scenario = scenario;
		currentTimeBinEndTime = timeBinSize;
	}

	public double getCurrentTimeBinEndTime() {
		return currentTimeBinEndTime;
	}

	public void setCurrentTimeBinEndTime(double currentTimeBinEndTime) {
		this.currentTimeBinEndTime = currentTimeBinEndTime;
	}

	public Map<Id<Link>, CongestionLinkInfo> getCongestionLinkInfos() {
		return linkId2info;
	}

	public int getWRITE_OUTPUT_ITERATION() {
		return writeOutputIteration;
	}

	public double getTIME_BIN_SIZE() {
		return timeBinSize;
	}

	public Map<Id<Vehicle>, Id<Person>> getVehicleId2personId() {
		return vehicleId2personId;
	}
	
	public DelayInternalizationApproach getINTERNALIZATION_APPROACH() {
		return internalizationApproach;
	}

	public Scenario getScenario() {
		return scenario;
	}

}

