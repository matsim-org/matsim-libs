/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freightReceiver;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;

public class UsecasesReceiverScoringFunctionFactory implements ReceiverScoringFunctionFactory {

    public UsecasesReceiverScoringFunctionFactory() {
    }

	@Override
	public ScoringFunction createScoringFunction(Receiver receiver) {
		SumScoringFunction sscorfunc = new SumScoringFunction();


		MoneyScoring carriertoReceiverCostAllocation = new CarriertoReceiverCostAllocation();
		sscorfunc.addScoringFunction(carriertoReceiverCostAllocation);

		return sscorfunc;
	}

	static class CarriertoReceiverCostAllocation implements MoneyScoring {

		private double cost = 0.0;

	 public void reset(){
			this.cost = 0.0;
		}

		@Override
		public void finish() {

		}


		@Override
		public double getScore() {
			return this.cost;
		}

		/*
		 * Adds the carrier cost to the receiver cost based on the number of receivers it serves. Currently only two receivers...this should be updated.
		 */
		@Override
		public void addMoney(double amount) {
			LogManager.getLogger(UsecasesReceiverScoringFunctionFactory.class).error("Where is this used?!");
			this.cost += amount;
		}
	}

}
