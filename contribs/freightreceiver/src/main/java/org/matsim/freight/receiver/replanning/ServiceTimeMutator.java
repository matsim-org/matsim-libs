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

package org.matsim.freight.receiver.replanning;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.freight.receiver.Order;
import org.matsim.freight.receiver.ReceiverOrder;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

/**
 * Changes the service time of a receivers' orders.
 *
 * @author jwjoubert, wlbean
 */

final class ServiceTimeMutator implements GenericPlanStrategyModule<ReceiverPlan> {
	private final Logger log = LogManager.getLogger(ServiceTimeMutator.class);

	private final double timeStep;
	private final double range;
	private final boolean increase;

	/**
	 * This class changes the service time of a receivers' orders with the
	 * specified time. If {@link #increase} is true, the service time will increase
	 * until the max allowed duration is reached. Conversely, if increase is
	 * false, the service time will decrease until the minimum duration is reached.
	 */
	public ServiceTimeMutator(double mutationTime, double mutationRange, boolean increase) {
		this.timeStep = mutationTime;
		this.range = mutationRange;
		this.increase = increase;
	}


	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void handlePlan(ReceiverPlan receiverPlan) {
		log.warn("entering handlePlan with increase=" + increase);

		/* Create list of receiver orders. */
		for (ReceiverOrder ro : receiverPlan.getReceiverOrders()) {

			/* Increase or decrease the service time with specified value until range min or range max is reached.*/
			for (Order order : ro.getReceiverProductOrders()) {

				double duration = order.getServiceDuration();

				if (increase) {
					duration = Math.min(duration + timeStep, range);
				} else {
					duration = Math.max(duration - timeStep, range);
				}
				order.setServiceDuration(duration);
			}
		}
	}

	@Override
	public void finishReplanning() {
	}

}
