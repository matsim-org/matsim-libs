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

public class End implements TourActivity, TourState {

	private String locationId;

	private double practical_earliestOperationStartTime;

	private double practical_latestOperationStartTime;

	private int currentLoad;

	private double currentCost;

	private TourStateSnapshot tourStateSnapshot = new TourStateSnapshot();

	public End(String locationId) {
		super();
		this.locationId = locationId;
	}

	public End(End end) {
		this.locationId = end.getLocationId();
		practical_earliestOperationStartTime = end
				.getEarliestOperationStartTime();
		practical_latestOperationStartTime = end.getLatestOperationStartTime();
		this.currentLoad = end.getCurrentLoad();
		this.currentCost = end.getCurrentCost();
		this.tourStateSnapshot = new TourStateSnapshot(
				end.getTourStateSnapshot());
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

	@Override
	public String toString() {
		return "End" + " @ " + getLocationId();
	}

	@Override
	public double getCurrentCost() {
		return currentCost;
	}

	@Override
	public void setCurrentCost(double cost) {
		this.currentCost = cost;
	}

	@Override
	public TourActivity duplicate() {
		return new End(this);
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
