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

package org.matsim.freight.receiver.collaboration;

import org.matsim.freight.receiver.Order;
import org.matsim.freight.receiver.ReceiverOrder;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;


/**
 * Changes the delivery frequency of a receiver and calculates new order sizes
 * for the receiver.
 *
 * @author wlbean
 *
 */

public class OrderSizeMutator implements GenericPlanStrategyModule<ReceiverPlan> {
	private boolean increase;

	/**
	 * This class changes the delivery frequency of a receiver by either
	 * increasing (if increase is true) or decreasing (if increase
	 * is false) the weekly delivery frequency with one day.
	 *
	 * @param increase
	 */

	public OrderSizeMutator(boolean increase){
		this.increase = increase;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {

	}

	@Override
	public void handlePlan(ReceiverPlan receiverPlan) {


		/* Create list of receiver orders. */
		for (ReceiverOrder ro : receiverPlan.getReceiverOrders()){

			/* Increase or decrease the number of deliveries per week with specified value until either 1 day (in case of decrease) or 5 days (in case of increase) is reached.*/
			for(Order order: ro.getReceiverProductOrders()){

				double numDel = order.getNumberOfWeeklyDeliveries();
				double sdemand;
				double random = MatsimRandom.getLocalInstance().nextDouble();
				double newNumDel;
				double weekdemand = order.getOrderQuantity();
				double pdeliver = numDel/5;

			if (increase == true){
					if (numDel + 1 <= 5){
						newNumDel = numDel + 1;
						pdeliver = newNumDel/5;
					} else {
						newNumDel = numDel;
						pdeliver = newNumDel/5;
					}

					if (random <= pdeliver){
						sdemand = weekdemand/newNumDel;
					} else sdemand = 0;

			} else {

				if (numDel - 1 >= 1){
					newNumDel = numDel - 1;
					pdeliver = newNumDel/5;
				} else {
					newNumDel = numDel;
					pdeliver = newNumDel/5;
				}


				if (random <= pdeliver){
					sdemand = weekdemand/newNumDel;
				} else sdemand = 0;
			}

			order.setNumberOfWeeklyDeliveries(newNumDel);
			order.setDailyOrderQuantity(sdemand);

			}
		}
	}


	@Override
	public void finishReplanning() {

	}

}
