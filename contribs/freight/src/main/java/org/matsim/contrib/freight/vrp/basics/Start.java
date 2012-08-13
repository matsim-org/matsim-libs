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



public class Start implements TourActivity, TourState{

	private String locationId;
	
	private double practical_earliestOperationStartTime;

	private double practical_latestOperationStartTime;

	private int currentLoad;

	private double currentCost;

	private TourStateSnapshot tourStateSnapshot = new TourStateSnapshot();
	
	public Start(String locationId) {
		super();
		this.locationId = locationId;
	}

	public Start(Start start) {
		this.locationId = start.getLocationId();
		practical_earliestOperationStartTime = start.getEarliestOperationStartTime();
		practical_latestOperationStartTime = start.getLatestOperationStartTime();
		this.currentLoad = start.getCurrentLoad();
		this.currentCost = start.getCurrentCost();
		this.tourStateSnapshot = new TourStateSnapshot(start.getTourStateSnapshot());
	}

	@Override
	public void setEarliestOperationStartTime(double early) {
		practical_earliestOperationStartTime = early;	
	}

	@Override
	public double getEarliestOperationStartTime() {
		return practical_earliestOperationStartTime;
	}

	@Override
	public double getLatestOperationStartTime() {
		return practical_latestOperationStartTime;
	}

	@Override
	public void setLatestOperationStartTime(double late) {
		practical_latestOperationStartTime = late;
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
		return "Start" + " @ "+ getLocationId();
	}

	@Override
	public double getCurrentCost() {
		return currentCost;
	}

	@Override
	public void setCurrentCost(double cost) {
		currentCost = cost;
	}

	@Override
	public TourActivity duplicate() {
		return new Start(this);
	}

	@Override
	public TourStateSnapshot getTourStateSnapshot() {
		return this.tourStateSnapshot;
	}

	@Override
	public void setTourStateSnapshot(TourStateSnapshot snapshot) {
		this.tourStateSnapshot = snapshot;
		
	}
	
}
