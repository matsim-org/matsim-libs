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
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author droeder
 *
 */
public class AnalysisVehicle {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(AnalysisVehicle.class);
	private double seatsOccupied;
	private double capacity;
	private Id locationId;
	private Id id;
	private Id lineId;
	private Id routeId;
	private int stopIndex = -1;

	public AnalysisVehicle(Id id, Id locationId, double capacity, Id lineId, Id routeId) {
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
	
	public Id getStopIndexId(){
		return new IdImpl(this.stopIndex);
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
	public final Id getLineId() {
		return lineId;
	}

	/**
	 * @return the routeId
	 */
	public final Id getRouteId() {
		return routeId;
	}
	
	
	
}

