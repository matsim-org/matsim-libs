/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.basics;


/**
 * 
 * @author stefan schroeder
 *
 */

public class Vehicle{
	
	private int capacity;
	
	private String locationId;
	
	private String id;
	
	private double earliestDeparture = 0.0;
	
	private double latestArrival = Double.MAX_VALUE;

	public double getEarliestDeparture() {
		return earliestDeparture;
	}

	public void setEarliestDeparture(double earliestDeparture) {
		this.earliestDeparture = earliestDeparture;
	}

	public double getLatestArrival() {
		return latestArrival;
	}

	public void setLatestArrival(double latestArrival) {
		this.latestArrival = latestArrival;
	}

	public Vehicle(String id, String locationId, int capacity) {
		super();
		this.capacity = capacity;
		this.locationId = locationId;
		this.id = id;
	}

	public String getLocationId() {
		return locationId;
	}

	public String getId() {
		return id;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	public int getCapacity() {
		return capacity;
	}
}
