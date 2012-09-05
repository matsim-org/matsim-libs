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

package playground.andreas.P2.scoring.operator;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.TransitDriverStartsEvent;

/**
 *
 * Holds all information needed to calculate the cost for this vehicle.
 * Note, this container does not know, whether there are mulptiple instances
 * of the container for a single vehicle or note. Consider this, when adding
 * up all containers, especially the fixed costs per day.
 *
 * @author aneumann
 *
 */
public class OperatorCostContainer {
	
	private final double costPerVehicleAndDay;
	private final double expensesPerMeter;
	private TransitDriverStartsEvent transitDriverStartsE;
	
	private double meterTravelled = 0.0;
	
	public OperatorCostContainer(double costPerVehicleAndDay, double expensesPerMeter){
		this.costPerVehicleAndDay = costPerVehicleAndDay;
		this.expensesPerMeter = expensesPerMeter;
	}

	public void handleTransitDriverStarts(TransitDriverStartsEvent transitDriverStartsE) {
		this.transitDriverStartsE = transitDriverStartsE;
	}

	public void addDistanceTravelled(double meterTravelled){
		this.meterTravelled  += meterTravelled;
	}
	
	public double getFixedCostPerDay(){
		return this.costPerVehicleAndDay;
	}
	
	public double getRunningCost(){
		return this.expensesPerMeter * this.meterTravelled;
	}
	
	public Id getVehicleId(){
		return this.transitDriverStartsE.getVehicleId();
	}
}
