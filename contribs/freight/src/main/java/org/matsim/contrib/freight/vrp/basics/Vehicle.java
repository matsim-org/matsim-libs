/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
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
