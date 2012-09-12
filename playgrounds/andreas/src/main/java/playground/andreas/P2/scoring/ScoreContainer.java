/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.P2.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.andreas.P2.scoring.fare.StageContainer;
import playground.andreas.P2.scoring.fare.TicketMachine;
import playground.andreas.P2.scoring.operator.OperatorCostContainer;

/**
 * Simple container class collecting all incomes and expenses for one single vehicle.
 * 
 * @author aneumann
 *
 */
public class ScoreContainer {
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(ScoreContainer.class);

	private final Id vehicleId;
	private final TicketMachine ticketMachine;
	private boolean isFirstTour = true;
	
	private int servedTrips = 0;
	private double costs = 0;
	private double earnings = 0;
	
	public ScoreContainer(Id vehicleId, TicketMachine ticketMachine) {
		this.vehicleId = vehicleId;
		this.ticketMachine = ticketMachine;
	}

	public void handleStageContainer(StageContainer stageContainer) {
		this.servedTrips++;
		this.earnings += this.ticketMachine.getFare(stageContainer);
	}

	public void handleOperatorCostContainer(OperatorCostContainer operatorCostContainer) {
		if (this.isFirstTour) {
			this.costs += operatorCostContainer.getFixedCostPerDay();
			this.isFirstTour = false;
		}
		this.costs += operatorCostContainer.getRunningCost();
	}

	public double getTotalRevenue(){
		return this.earnings - this.costs;
	}
	
	public double getTotalRevenuePerPassenger(){
		if(this.servedTrips == 0){
			return Double.NaN;
		} else {
			return (this.earnings - this.costs) / this.servedTrips;
		}
	}
	
	public int getTripsServed(){
		return this.servedTrips;
	}
	
	@Override
	public String toString() {
		return "Paratransit vehicle " + this.vehicleId.toString() + " served " + this.servedTrips + " trips spending a total of " + this.costs + " vs. " + this.earnings + " earnings";
	}
}
