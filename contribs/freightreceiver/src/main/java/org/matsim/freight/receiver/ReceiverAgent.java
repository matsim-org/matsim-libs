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
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.core.scoring.ScoringFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * This keeps track of a single freight receiver during simulation.
 *
 * @author wlbean
 */

class ReceiverAgent {

	private final Receiver receiver;
	private final ScoringFunction scoringFunction;
	final private Logger log = LogManager.getLogger(ReceiverAgent.class);


	public ReceiverAgent(Receiver receiver, ScoringFunction receiverScoringFunction) {
		this.receiver = receiver;
		this.scoringFunction = receiverScoringFunction;
	}


	/**
	 * Score the receiver agent's selected order. This score reflects the receiver
	 * cost and is currently determined as the carrier's delivery cost to that
	 * receiver (based on the proportion of this receiver's orders in all the
	 * orders delivered by the carrier). This is not really realistic, and will
	 * be changed in the future.
	 * <p>
	 * FIXME: JWJ (23/6/2018): I'm not quite sure what the purpose of this
	 *  method is. I've updated it so that the plan's cost is simply the sum
	 *  of all individual order's costs.
	 *
	 * @author wlbean, jwjoubert
	 */
	public void scoreSelectedPlan(Scenario scenario, ReceiverCostAllocation costAllocation) {

		ReceiverPlan selectedPlan = receiver.getSelectedPlan();
		if (selectedPlan == null) {
			log.warn("No receiver plan is selected.");
			return;
		}

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		double score = 0.0;
		List<Id<Carrier>> carrierIds = new ArrayList<>();

		/* Each order may have a different carrier and therefore incur its own score. */
		for (ReceiverOrder order : selectedPlan.getReceiverOrders()) {
			Id<Carrier> carrierId = order.getCarrierId();
			if (!carrierIds.contains(order.getCarrierId())) {
				score += costAllocation.getScore(carriers.getCarriers().get(carrierId), receiver);
				carrierIds.add(carrierId);
			}
		}

		/* A negative 'score' is a negative money transaction, i.e. a cost.
		 * I cannot see how this can really be an income. It is more important
		 * to ensure consistency in how utility and costs are handled. */
		scoringFunction.addMoney(score);
		scoringFunction.finish();

		receiver.getSelectedPlan().setScore(scoringFunction.getScore());
		receiver.getAttributes().putAttribute(ReceiverUtils.ATTR_RECEIVER_SCORE, scoringFunction.getScore());
	}


	/**
	 * Returns the receiver agent's unique receiver id.
	 */
	public Id<Receiver> getId() {
		return receiver.getId();
	}

}
