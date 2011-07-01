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
package playground.andreas.P2.pbox;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Paratransit plan
 * 
 * @author aneumann
 *
 */
public class PPlan {

	private final static Logger log = Logger.getLogger(PPlan.class);
	
	private final Id id;

	private TransitLine line;
	private double score = Double.NaN;
	private int tripsServed = 0;

	private double startTime;
	private double endTime;
	
	private TransitStopFacility startStop;
	private TransitStopFacility endStop;

	private Set<Id> vehicleIds;
	
	public PPlan(Id id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Plan " + this.id + ", score: " + this.score + ", score/veh: " + (this.score / this.vehicleIds.size())
		+ ", trips: " + this.tripsServed + ", vehicles: " + this.vehicleIds.size()
		+ ", Operation time: " + Time.writeTime(this.startTime) + "-" + Time.writeTime(this.endTime)
		+ ", corridor from " + this.startStop.getId() + " to " + this.endStop.getId();
	}

	public Id getId() {
		return id;
	}

	public void setLine(TransitLine line) {
		this.line = line;
		this.vehicleIds = new TreeSet<Id>();
		for (TransitRoute route : this.line.getRoutes().values()) {
			for (Departure departure : route.getDepartures().values()) {
				this.vehicleIds.add(departure.getVehicleId());
			}
		}		
	}
	
	public TransitLine getLine(){
		return this.line;
	}
	
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public void setStartStop(TransitStopFacility startStop) {
		this.startStop = startStop;
	}

	public TransitStopFacility getStartStop() {
		return startStop;
	}

	public void setEndStop(TransitStopFacility endStop) {
		this.endStop = endStop;
	}

	public TransitStopFacility getEndStop() {
		return endStop;
	}

	public void setScore(double totalLineScore) {
		this.score = totalLineScore;		
	}

	public double getScore() {
		return this.score;
	}
	
	public double getScorePerVehicle() {
		return (this.score / this.vehicleIds.size());
	}

	public Set<Id> getVehicleIds() {
		return vehicleIds;
	}

	public void setTripsServed(int tripsServed) {
		this.tripsServed = tripsServed;
	}

	public int getTripsServed() {
		return tripsServed;
	}

	public boolean isSameButVehSize(PPlan testPlan) {
		if(!this.startStop.getId().toString().equalsIgnoreCase(testPlan.getStartStop().getId().toString())){
			return false;
		}
		
		if(!this.endStop.getId().toString().equalsIgnoreCase(testPlan.getEndStop().getId().toString())){
			return false;
		}
		
		if(this.startTime != testPlan.getStartTime()){
			return false;
		}
		
		if(this.endTime != testPlan.getEndTime()){
			return false;
		}
		
		return true;
	}	
}