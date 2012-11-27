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

public class Delivery implements TourActivity, JobActivity, TourState {

	private final Job job;

	private final String locationId;

	private final double theoretical_earliestOperationStartTime;
	
	private final double theoretical_latestOperationStartTime;

	private final int demand;

	private final double serviceTime;
	
	private double practical_earliestOperationStartTime;

	private double practical_latestOperationStartTime;
		
	private int currentLoad;

	private double currentCost;

	private TourStateSnapshot tourStateSnapshot = new TourStateSnapshot();

	public Delivery(Shipment shipment) {
		super();
		this.job = shipment;
		this.locationId = shipment.getToId();
		this.demand = shipment.getCapacityDemand();
		this.serviceTime = shipment.getDeliveryServiceTime();
		theoretical_earliestOperationStartTime = shipment.getDeliveryTW().getStart();
		theoretical_latestOperationStartTime = shipment.getDeliveryTW().getEnd();
		practical_earliestOperationStartTime = shipment.getDeliveryTW().getStart();
		practical_latestOperationStartTime = shipment.getDeliveryTW().getEnd();
	}

	public Delivery(Service deliveryService) {
		this.job = deliveryService;
		this.locationId = deliveryService.getLocationId();
		this.demand = deliveryService.getCapacityDemand();
		this.serviceTime = deliveryService.getServiceTime();
		practical_earliestOperationStartTime = deliveryService.getEarliestServiceTime();
		practical_latestOperationStartTime = deliveryService.getLatestServiceTime();
		theoretical_earliestOperationStartTime = deliveryService.getEarliestServiceTime();
		theoretical_latestOperationStartTime = deliveryService.getLatestServiceTime();
	}

	public Delivery(Delivery delivery) {
		this.job = delivery.getJob();
		this.locationId = delivery.getLocationId();
		this.demand = delivery.getCapacityDemand() * -1;
		this.serviceTime = delivery.getOperationTime();
		practical_earliestOperationStartTime = delivery.getEarliestOperationStartTime();
		practical_latestOperationStartTime = delivery.getLatestOperationStartTime();
		theoretical_earliestOperationStartTime = delivery.getTheoreticalEarliestOperationStartTime();
		theoretical_latestOperationStartTime = delivery.getTheoreticalLatestOperationStartTime();
		this.currentLoad = delivery.getCurrentLoad();
		this.currentCost = delivery.getCurrentCost();
		this.tourStateSnapshot = new TourStateSnapshot(delivery.getTourStateSnapshot());
	}
	
	

	public double getTheoreticalEarliestOperationStartTime() {
		return theoretical_earliestOperationStartTime;
	}

	public double getTheoreticalLatestOperationStartTime() {
		return theoretical_latestOperationStartTime;
	}

	@Override
	public int getCapacityDemand() {
		return -1 * demand;
	}

	@Override
	public double getOperationTime() {
		return serviceTime;
	}

	@Override
	public String getLocationId() {
		return locationId;
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
	public Job getJob() {
		return job;
	}

	@Override
	public int getCurrentLoad() {
		return this.currentLoad;
	}

	@Override
	public void setCurrentLoad(int load) {
		this.currentLoad = load;

	}

	@Override
	public String toString() {
		return "Delivery" + " of " + demand + " units @ " + getLocationId()
				+ " @ practTW(" + round(practical_earliestOperationStartTime)
				+ "," + round(practical_latestOperationStartTime) + ")";
	}

	private String round(double time) {
		if (time == Double.MAX_VALUE) {
			return "oo";
		}
		return "" + Math.round(time);
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
		return new Delivery(this);
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
