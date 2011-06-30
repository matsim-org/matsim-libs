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
package playground.andreas.P2.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Simple container class holding scoring information for one vehicle
 * 
 * @author aneumann
 *
 */
public class ScoreContainer {
	
	private final static Logger log = Logger.getLogger(ScoreContainer.class);

	private final Id vehicleId;
	private final double earningsPerMeterAndPassenger;
	private final double expensesPerMeter;
	
	private int servedTrips = 0;
	private double costs = 0;
	private double earnings = 0;
		
	int passengersCurrentlyInVeh = 0;
	
	public ScoreContainer(Id vehicleId, double earningsPerMeterAndPassenger, double expensesPerMeter){
		this.vehicleId = vehicleId;
		this.earningsPerMeterAndPassenger = earningsPerMeterAndPassenger;
		this.expensesPerMeter = expensesPerMeter;
	}
	
	public void addPassenger(){
		this.passengersCurrentlyInVeh++;
	}
	
	public void removePassenger(){
		this.passengersCurrentlyInVeh--;
		this.servedTrips++;
	}
	
	public void handleLinkTravelled(Link link){
		this.costs += link.getLength() * this.expensesPerMeter;
		this.earnings += link.getLength() * this.earningsPerMeterAndPassenger * this.passengersCurrentlyInVeh;
	}
	
	public double getTotalRevenue(){
		return this.earnings - this.costs;
	}
	
	public double getTotalRevenuePerPassenger(){
		return (this.earnings - this.costs) / this.servedTrips;
	}
	
	public int getTripsServed(){
		return this.servedTrips;
	}
	
	@Override
	public String toString() {
		return "Paratransit vehicle " + this.vehicleId.toString() + " served " + this.servedTrips + " trips spending a total of " + this.costs + " vs. " + this.earnings + " earnings";
	}
}
