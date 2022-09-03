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

package org.matsim.contrib.minibus.hook;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.fare.StageContainer;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.vehicles.Vehicle;

/**
 * Simple container class collecting all incomes and expenses for one single vehicle.
 *
 * @author aneumann
 */
final class PScoreContainer {

    @SuppressWarnings("unused")
    private final static Logger log = LogManager.getLogger(PScoreContainer.class);

    private final Id<Vehicle> vehicleId;
    private final TicketMachineI ticketMachine;
    private boolean isFirstTour = true;

    private int servedTrips = 0;
    private double costs = 0;
    private double earnings = 0;

    PScoreContainer(Id<Vehicle> vehicleId, TicketMachineI ticketMachine) {
        this.vehicleId = vehicleId;
        this.ticketMachine = ticketMachine;
    }

    void handleStageContainer(StageContainer stageContainer) {
        this.servedTrips++;
        this.earnings += this.ticketMachine.getFare(stageContainer);
    }

    void handleOperatorCostContainer(OperatorCostContainer operatorCostContainer) {
        if (this.isFirstTour) {
            this.costs += operatorCostContainer.getFixedCostPerDay();
            this.isFirstTour = false;
        }
        this.costs += operatorCostContainer.getRunningCostDistance();
        this.costs += operatorCostContainer.getRunningCostTime();

		/* Since the monetary transactions may include non-fare income, we
		specifically convert them to costs here. */
        double value = operatorCostContainer.getOtherMonetaryTransactions();
        if (value < 0.0) {
            this.costs += -value;
        } else{
            this.earnings += value;
        }
    }

    /**
     * @deprecated use {@link #getProfit()}
     */
    @Deprecated // use getProfit()
    public double getTotalRevenue() {
        // has to be "profit".  Revenue is same as earnings.  kai, jul'21
        return this.getProfit();
    }

    public double getProfit() {
        return this.earnings - this.costs;
    }

//    public double getTotalRevenuePerPassenger() {
//        if (this.servedTrips == 0) {
//            return Double.NaN;
//        } else {
//            return (this.earnings - this.costs) / this.servedTrips;
//        }
//    }
    // never used, and can be computed from other public getters of this class.  kai, jul'21

    public int getTripsServed() {
        return this.servedTrips;
    }

    @Override
    public String toString() {
        return "Paratransit vehicle " + this.vehicleId.toString() + " served " + this.servedTrips + " trips spending a total of " + this.costs + " vs. " + this.earnings + " earnings";
    }
}
