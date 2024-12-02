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

package org.matsim.freight.receiver;

import org.apache.logging.log4j.LogManager;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;

class ReceiverScoringFunctionFactoryMoneyOnly implements ReceiverScoringFunctionFactory {

	ReceiverScoringFunctionFactoryMoneyOnly() {
	}

	@Override
	public ScoringFunction createScoringFunction(Receiver receiver) {
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CarrierToReceiverCostAllocation());
		return sumScoringFunction;
	}

	static class CarrierToReceiverCostAllocation implements MoneyScoring {

		private double moneyBalance = 0.0;

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return this.moneyBalance;
		}

		@Override
		public void addMoney(double amount) {
			if (amount < 0) {
				LogManager.getLogger(ReceiverScoringFunctionFactoryMoneyOnly.class).warn("What?! The receiver is getting paid for a delivery?! Make sure this is what you want.");
			}
//			LogManager.getLogger(ReceiverScoringFunctionFactoryMoneyOnly.class).error("Where is this used?!");
			this.moneyBalance += amount;
		}
	}

}
