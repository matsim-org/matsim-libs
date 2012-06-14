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


public class Pickup implements TourActivity, JobActivity{

	private Shipment shipment;
	
	private double practical_earliestArrivalTime;
	
	private double practical_latestArrivalTime;

	private int currentLoad;
	
	public Pickup(Shipment shipment) {
		super();
		this.shipment = shipment;
		practical_earliestArrivalTime = shipment.getPickupTW().getStart();
		practical_latestArrivalTime = shipment.getPickupTW().getEnd();
	}
	
	@Override
	public String getType() {
		return "Pickup";
	}

	public int getCapacityDemand(){
		return shipment.getSize();
	}

	@Override
	public double getOperationTime() {
		return shipment.getPickupServiceTime();
	}

	public String getLocationId() {
		return shipment.getFromId();
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
	public Job getJob() {
		return shipment;
	}

	@Override
	public int getCurrentLoad() {
		return this.currentLoad;
	}

	@Override
	public void setCurrentLoad(int load) {
		this.currentLoad = load;
	}

}
