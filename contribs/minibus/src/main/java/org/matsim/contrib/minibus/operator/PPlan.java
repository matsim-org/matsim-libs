/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.operator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * The operator's plan
 * 
 * @author aneumann
 *
 */
public final class PPlan implements Comparable<PPlan>{

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(PPlan.class);
	
	private final Id<PPlan> planId;
	private final String creator;
	private Id<PPlan> parentId;

	private TransitLine line;
	private double score = Double.NaN;
	private int tripsServed = 0;

	private double startTime;
	private double endTime;
	private int nVehicles;
	
	private ArrayList<TransitStopFacility> stopsToBeServed;

	private Set<Id<Vehicle>> vehicleIds;

	
	public PPlan(Id<PPlan> planId, String creator, Id<PPlan> parentId) {
		this.planId = planId;
		this.creator = creator;
		this.parentId = parentId;
	}
	
	@Override
	public String toString() {
		StringBuffer sB = new StringBuffer();
		sB.append("Plan " + this.planId + ", score: " + this.score + ", score/veh: " + this.getScorePerVehicle()
				+ ", trips: " + this.tripsServed + ", vehicles: " + this.vehicleIds.size()
				+ ", Operation time: " + Time.writeTime(this.startTime) + "-" + Time.writeTime(this.endTime)
				+ ", Stops: ");
		
		for (TransitStopFacility stop : this.stopsToBeServed) {
			sB.append(stop.getId()); sB.append(", ");
		}
		
		return  sB.toString();
	}
	
	public String toString(double budget) {		
		StringBuffer sB = new StringBuffer();
		sB.append("Plan " + this.planId + ", score: " + this.score + ", score/veh: " + this.getScorePerVehicle()
				+ ", trips: " + this.tripsServed + ", vehicles: " + this.vehicleIds.size()
				+ ", Operation time: " + Time.writeTime(this.startTime) + "-" + Time.writeTime(this.endTime)
				+ ", Stops: ");
		
		for (TransitStopFacility stop : this.stopsToBeServed) {
			sB.append(stop.getId()); sB.append(", ");
		}
		
		sB.append("line budget " + budget);
		
		return  sB.toString();
	}

	public Id<PPlan> getId() {
		return this.planId;
	}
	
	public String getCreator() {
		return this.creator;
	}
	
	public Id<PPlan> getParentId() {
		return this.parentId;
	}

	public TransitLine getLine(){
		return this.line;
	}

	public void setLine(TransitLine line) {
		this.line = line;
		this.vehicleIds = new TreeSet<>();
		for (TransitRoute route : this.line.getRoutes().values()) {
			for (Departure departure : route.getDepartures().values()) {
				this.vehicleIds.add(departure.getVehicleId());
			}
		}
		this.nVehicles = this.vehicleIds.size();
	}
	
	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}
	
	public int getNVehicles(){
		return this.nVehicles;
	}
	
	public void setNVehicles(int nVehicles){
		this.nVehicles = nVehicles;
	}

	public ArrayList<TransitStopFacility> getStopsToBeServed() {
		return stopsToBeServed;
	}
	
	public void setStopsToBeServed(ArrayList<TransitStopFacility> stopsToBeServed) {
		this.stopsToBeServed = stopsToBeServed;
	}

	public double getScore() {
		return this.score;
	}

	public void setScore(double totalLineScore) {
		this.score = totalLineScore;		
	}

	public double getScorePerVehicle() {
		return (this.score / this.vehicleIds.size());
	}
	
	public double getPlannedScorePerVehicle(){
		if (this.nVehicles == 0) {
			return 0.0;
		} else {
			return (this.score / this.nVehicles);
		}
	}

	public Set<Id<Vehicle>> getVehicleIds() {
		return vehicleIds;
	}

	public int getTripsServed() {
		return tripsServed;
	}

	public void setTripsServed(int tripsServed) {
		this.tripsServed = tripsServed;
	}

	public boolean isSameButVehSize(PPlan testPlan) {
		
		if(this.startTime != testPlan.getStartTime()){
			return false;
		}
		
		if(this.endTime != testPlan.getEndTime()){
			return false;
		}

		if (testPlan.getStopsToBeServed().size() != this.getStopsToBeServed().size()) {
			return false;
		}
		
		for (int i = 0; i < this.stopsToBeServed.size(); i++) {
			if(!this.stopsToBeServed.get(i).getId().toString().equalsIgnoreCase(testPlan.getStopsToBeServed().get(i).getId().toString())){
				return false;
			}			
		}
		
		return true;
	}
	
	public boolean isSameButOperationTime(PPlan testPlan) {
		for (int i = 0; i < this.stopsToBeServed.size(); i++) {
			if(!this.stopsToBeServed.get(i).getId().toString().equalsIgnoreCase(testPlan.getStopsToBeServed().get(i).getId().toString())){
				return false;
			}
		}
		
		if(this.nVehicles != testPlan.getNVehicles()){
			return false;
		}
		
		return true;
	}

	@Override
	public int compareTo(PPlan plan) {
	    if (plan.getScorePerVehicle() > this.getScorePerVehicle()) {
	      return 1;
	    }
	    if (plan.getScorePerVehicle() < this.getScorePerVehicle()) {
	      return -1;
	    }
//	    if (plan.getScorePerVehicle() == this.getScorePerVehicle()) {
		      return 0;
//	    }
	}
}