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
package playground.vsp.analysis.modules.ptRoutes2paxAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * @author droeder
 *
 */
public class AnalysisVehicle {

	private static final Logger log = Logger.getLogger(AnalysisVehicle.class);
	private double seatsOccupied;
	private double capacity;
	private Id locationId;
	private Id id;
	private Id<TransitLine> lineId;
	private Id<TransitRoute> routeId;
	private int stopIndex = -1;

	public AnalysisVehicle(Id id, Id locationId, double capacity, Id<TransitLine> lineId, Id<TransitRoute> routeId) {
		this.id = id;
		this.locationId = locationId;
		this.capacity = capacity;
		this.seatsOccupied = 0.;
		this.lineId = lineId;
		this.routeId = routeId;
	}
	
	public void personBoards(){
		this.seatsOccupied++;
		if(this.seatsOccupied > this.capacity){
			log.warn("vehicle " + this.id + ", the number of seats occupied (" + this.seatsOccupied + ") is bigger than the capacity (" + this.capacity + ")!");
		}
	}
	
	public void personAlights(){
		this.seatsOccupied--;
		if(this.seatsOccupied < 0){
			log.warn("vehicle " + this.id + ", less than zero seats are occupied. This should never happen!");
		}
	}
	
	public Id<Link> getStopIndexId(){
		return Id.create(this.stopIndex, Link.class);
	}

	/**
	 * @return the locationId
	 */
	public final Id getLocationId() {
		return locationId;
	}

	/**
	 * sets the real location id and increases the index of the current stop...
	 * @param locationId the locationId to set
	 */
	public final void setLocationId(Id locationId) {
		this.locationId = locationId;
		this.stopIndex ++;
	}

	/**
	 * @return the seatsOccupied
	 */
	public final double getSeatsOccupied() {
		return seatsOccupied;
	}

	/**
	 * @return the capacity
	 */
	public final double getCapacity() {
		return capacity;
	}

	/**
	 * @return the id
	 */
	public final Id getId() {
		return id;
	}

	/**
	 * @return the lineId
	 */
	public final Id<TransitLine> getLineId() {
		return lineId;
	}

	/**
	 * @return the routeId
	 */
	public final Id<TransitRoute> getRouteId() {
		return routeId;
	}
	
	
	
}

