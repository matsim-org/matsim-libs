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
	
	private final double theoretical_earliestOperationStartTime;
	
	private final double theoretical_latestOperationStartTime;

	private int currentLoad;

	private double currentCost;

	private TourStateSnapshot tourStateSnapshot = new TourStateSnapshot();

	public End(String locationId, double theoreticalStart, double theoreticalEnd) {
		super();
		this.locationId = locationId;
		theoretical_earliestOperationStartTime = theoreticalStart;
		theoretical_latestOperationStartTime = theoreticalEnd;
		practical_earliestOperationStartTime = theoreticalStart;
		practical_latestOperationStartTime = theoreticalEnd;
	}

	public End(End end) {
		this.locationId = end.getLocationId();
		practical_earliestOperationStartTime = end.getEarliestOperationStartTime();
		practical_latestOperationStartTime = end.getLatestOperationStartTime();
		theoretical_earliestOperationStartTime = end.getTheoreticalEarliestOperationStartTime();
		theoretical_latestOperationStartTime = end.getTheoreticalLatestOperationStartTime();
		this.currentLoad = end.getCurrentLoad();
		this.currentCost = end.getCurrentCost();
		this.tourStateSnapshot = new TourStateSnapshot(end.getTourStateSnapshot());
	}

	public double getTheoreticalEarliestOperationStartTime() {
		return theoretical_earliestOperationStartTime;
	}

	public double getTheoreticalLatestOperationStartTime() {
		return theoretical_latestOperationStartTime;
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
		return "End" + " @ " + getLocationId() 
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
