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

package playground.andreas.P2.replanning;

import java.util.ArrayList;
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

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(PPlan.class);
	
	private final Id id;
	private final String creator;

	private TransitLine line;
	private double score = Double.NaN;
	private int tripsServed = 0;

	private double startTime;
	private double endTime;
	private int nVehicles;
	
	private ArrayList<TransitStopFacility> stopsToBeServed;

	private Set<Id> vehicleIds;
	
	public PPlan(Id id, String creator) {
		this.id = id;
		this.creator = creator;
	}
	
	public PPlan(Id id, String creator, ArrayList<TransitStopFacility> stopsToBeServed, double startTime, double endTime){
		this.id = id;
		this.creator = creator;
		this.stopsToBeServed = stopsToBeServed;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public PPlan(Id id, String creator, PPlan oldPlan){
		this.id = id;
		this.creator = creator;
		this.stopsToBeServed = oldPlan.getStopsToBeServed();
		this.startTime = oldPlan.getStartTime();
		this.endTime = oldPlan.getEndTime();
		this.line = oldPlan.getLine();
	}

	@Override
	public String toString() {
		StringBuffer sB = new StringBuffer();
		sB.append("Plan " + this.id + ", score: " + this.score + ", score/veh: " + (this.score / this.vehicleIds.size())
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
		sB.append("Plan " + this.id + ", score: " + this.score + ", score/veh: " + (this.score / this.vehicleIds.size())
				+ ", trips: " + this.tripsServed + ", vehicles: " + this.vehicleIds.size()
				+ ", Operation time: " + Time.writeTime(this.startTime) + "-" + Time.writeTime(this.endTime)
				+ ", Stops: ");
		
		for (TransitStopFacility stop : this.stopsToBeServed) {
			sB.append(stop.getId()); sB.append(", ");
		}
		
		sB.append("line budget " + budget);
		
		return  sB.toString();
	}

	public Id getId() {
		return this.id;
	}
	
	public String getCreator() {
		return this.creator;
	}

	public TransitLine getLine(){
		return this.line;
	}

	public void setLine(TransitLine line) {
		this.line = line;
		this.vehicleIds = new TreeSet<Id>();
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

	public Set<Id> getVehicleIds() {
		return vehicleIds;
	}

	public int getTripsServed() {
		return tripsServed;
	}

	public void setTripsServed(int tripsServed) {
		this.tripsServed = tripsServed;
	}

	public boolean isSameButVehSize(PPlan testPlan) {
		
		if (testPlan.getStopsToBeServed().size() != this.getStopsToBeServed().size()) {
			return false;
		}
		
		for (int i = 0; i < this.stopsToBeServed.size(); i++) {
			if(!this.stopsToBeServed.get(i).getId().toString().equalsIgnoreCase(testPlan.getStopsToBeServed().get(i).getId().toString())){
				return false;
			}			
		}
		
		if(this.startTime != testPlan.getStartTime()){
			return false;
		}
		
		if(this.endTime != testPlan.getEndTime()){
			return false;
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
}