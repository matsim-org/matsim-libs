/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips.distance;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class DistAnalysisVehicle {
	
	private Id vehicleId;
	private List<DistAnalysisAgent> passengers;
	private DistAnalysisTransitRoute route;
	private double distance = 0;
	private double totalPassengers = 0;

	/**
	 * @param vehicleId
	 */
	public DistAnalysisVehicle(Id vehicleId) {
		this.vehicleId = vehicleId;
		this.passengers = new ArrayList<DistAnalysisAgent>();
	}

	
	public Id getId(){
		return this.vehicleId;
	}
	
	/**
	 * register the route at the vehicle and removes the old one, if a new is given
	 * @param distAnalysisTransitRoute
	 */
	public void registerRoute(DistAnalysisTransitRoute route) {
		this.route = route;
	}

	/**
	 * add the agent to the passengerlist
	 * @param a
	 */
	public void enterVehicle(DistAnalysisAgent a) {
		this.passengers.add(a);
		this.totalPassengers++;
		this.route.countPassenger();
	}

	/**
	 * remove the agent from passengerlist if it is in
	 * @param a
	 */
	public boolean leaveVehicle(DistAnalysisAgent a) {
		if(this.passengers.remove(a)){
			return true;
		}else{
			return false;
		}
	}
	
	public void processLinkEnterEvent(double linkLength){
		this.distance += linkLength;
		for(DistAnalysisAgent passenger : this.passengers){
			passenger.passedLinkInPt(linkLength);
		}
		this.route.passedLink(linkLength, this.passengers.size());
	}


	/**
	 * @return the vehicleId
	 */
	public Id getVehicleId() {
		return vehicleId;
	}


	/**
	 * @return the distance
	 */
	public double getDistance() {
		return distance;
	}


	/**
	 * @return the totalPassengers
	 */
	public double getTotalPassengers() {
		return totalPassengers;
	}
	
	public String toString(boolean header){
		StringBuffer b = new StringBuffer();
		if(header){
			b.append("vehicleId;PassengerCnt;Dist[m]\n");
		}
		b.append(this.vehicleId.toString() + ";" + this.totalPassengers + ";" + this.distance + "\n");
		return b.toString();
	}
	
	@Override
	public String toString(){
		return this.toString(true);
	}
}
