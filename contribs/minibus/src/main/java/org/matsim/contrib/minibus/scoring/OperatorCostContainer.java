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

package org.matsim.contrib.minibus.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.vehicles.Vehicle;

/**
 *
 * Holds all information needed to calculate the cost for this vehicle. Note,
 * this container does not know, whether there are multiple instances of the
 * container for a single vehicle or note. Consider this, when adding up all
 * containers, especially the fixed costs per day.
 * 
 * There is one container for each stage of a vehicle indicated by a
 * {@link TransitDriverStartsEvent}.
 *
 * @author aneumann
 *
 */
final class OperatorCostContainer {

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(OperatorCostContainer.class);

	private final double costPerVehicleAndDay;
	private final double expensesPerMeter;
	private final double expensesPerSecond;
	private TransitDriverStartsEvent transitDriverStartsE;
	private PersonLeavesVehicleEvent transitDriverAlightsE;
	private VehicleAbortsEvent vehicleAbortE;

	private double meterTravelled = 0.0;

	public OperatorCostContainer(double costPerVehicleAndDay, double expensesPerMeter, double expensesPerSecond) {
		this.costPerVehicleAndDay = costPerVehicleAndDay;
		this.expensesPerMeter = expensesPerMeter;
		this.expensesPerSecond = expensesPerSecond;
	}

	// yyyy my intuition is that this class should be very much reduced, just
	// returning "cost", and handling arbitrary events, and figure
	// out the rest internally. That would make it much more flexible with respect
	// to replacement. kai, jan'17

	public void handleTransitDriverStarts(TransitDriverStartsEvent transitDriverStartsE1) {
		this.transitDriverStartsE = transitDriverStartsE1;
	}

	public void addDistanceTravelled(double meterTravelled1) {
		this.meterTravelled += meterTravelled1;
	}

	/**
	 * This terminates the stage
	 */
	public void handleTransitDriverAlights(PersonLeavesVehicleEvent event) {
		this.transitDriverAlightsE = event;
	}

	/**
	 * This terminates the stage if the driver is stuck. If the driver stucks there
	 * should not be any PersonLeavesVehicleEvent since the last
	 * TransitDriverStartsEvent, so there is either a PersonLeavesVehicleEvent or a
	 * PersonStuckEvent.
	 */
	public void handleVehicleAborts(VehicleAbortsEvent event) {
		this.vehicleAbortE = event;
	}

	public double getFixedCostPerDay() {
		return this.costPerVehicleAndDay;
	}

	public double getRunningCostDistance() {
		return this.expensesPerMeter * this.meterTravelled;
	}

	public double getRunningCostTime() {
		double timeInService = 0;
		
		if (this.transitDriverAlightsE != null) {
			if (this.vehicleAbortE != null) {
				throw new RuntimeException(
						"There is both a PersonLeavesVehicleEvent and a VehicleAbortsEvent for TransitDriverStartsEvent "
								+ this.transitDriverStartsE);
			}
			timeInService = this.transitDriverAlightsE.getTime() - this.transitDriverStartsE.getTime();
			
		} else if (this.vehicleAbortE != null) {
			timeInService = this.vehicleAbortE.getTime() - this.transitDriverStartsE.getTime();
			
		} else {
			throw new RuntimeException(
					"Neither PersonLeavesVehicleEvent nor VehicleAbortEvent found for TransitDriverStartsEvent "
							+ transitDriverStartsE.getDriverId() + ".");
		}
		return this.expensesPerSecond * timeInService;
	}

	public Id<Vehicle> getVehicleId() {
		return this.transitDriverStartsE.getVehicleId();
	}

	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("costPerVehicleAndDay " + costPerVehicleAndDay + "; ");
		strB.append("expensesPerMeter " + expensesPerMeter + "; ");
		strB.append("expensesPerSecond " + expensesPerSecond + "; ");
		strB.append("transitDriverStartsE " + transitDriverStartsE + "; ");
		strB.append("transitDriverAlightsE " + transitDriverAlightsE + "; ");
		strB.append("vehicleAbortE " + vehicleAbortE + "; ");
		strB.append("meterTravelled " + meterTravelled + "; ");
		return strB.toString();
	}

}
