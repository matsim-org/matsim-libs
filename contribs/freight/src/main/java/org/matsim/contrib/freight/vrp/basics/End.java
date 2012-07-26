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



public class End implements TourActivity{
	
	private String locationId;
	
	private double practical_earliestArrivalTime;

	private double practical_latestArrivalTime;

	private int currentLoad;

	private double currentCost;
	
	public End(String locationId) {
		super();
		this.locationId = locationId;
	}

	@Override
	public String getType() {
		return "End";
	}

	@Override
	public void setEarliestOperationStartTime(double early) {
		practical_earliestArrivalTime = early;	
	}

	@Override
	public double getEarliestOperationStartTime() {
		return practical_earliestArrivalTime;
	}

	@Override
	public double getLatestOperationStartTime() {
		return practical_latestArrivalTime;
	}

	@Override
	public void setLatestOperationStartTime(double late) {
		practical_latestArrivalTime = late;
	}

	@Override
	public String getLocationId() {
		return locationId;
	}

	@Override
	public double getOperationTime() {
		return 0.0;
	}

	@Override
	public int getCurrentLoad() {
		return this.currentLoad;
	}

	@Override
	public void setCurrentLoad(int load) {
		this.currentLoad = load;
		
	}
	
	public String toString(){
		return getType() + " @ "+ getLocationId();
	}

	@Override
	public double getCurrentCost() {
		return currentCost;
	}

	@Override
	public void setCurrentCost(double cost) {
		this.currentCost = cost;
	}
}
