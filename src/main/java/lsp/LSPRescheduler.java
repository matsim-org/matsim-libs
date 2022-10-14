/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp;

import lsp.shipment.LSPShipment;
import org.matsim.core.controler.events.ReplanningEvent;


/**
 * There is a possibility and also sometimes a need for rescheduling before each
 * new iteration. This need results from changes to the plan of the LSP during
 * the re-planning process. There, changes of the structure and/or number of the
 * active LogisticsSolutions or to the assignment of the LSPShipments to the
 * LogisticsSolutions can happen. Consequently, the transport operations that
 * result from this change in behavior of the corresponding LSP will also change
 * and thus the preparation of the simulation has to be redone.
 * <p>
 * The rescheduling process is triggered in the BeforeMobsimEvent of the following iteration which has
 * a LSPRescheduler as one of its listeners.
 * <p>
 * In this case, all LogisticsSolutions,
 * LogisticsSolutionElements and Resources are cleared of all shipments that
 * were assigned to them in the previous iteration in order to allow a new assignment.
 * <p>
 * Then, all LSPShipments of each LSP are assigned to the corresponding
 * LogisticsSolutions by the Assigner of the LSP in order to account for possible
 * changes in the LogisticsSolutions as well as in the assignment itself due to
 * the re-planning that took place before. After this assignment is done, the actual
 * scheduling takes place. In cases, where no re-planning takes place (further details of
 * the re-planning algorithm follow in 3.8), rescheduling will nevertheless take place.
 * This is reasonable for example in cases where also other traffic takes place on the
 * network, for example passenger traffic, and the network conditions change between
 * subsequent iterations of the simulation due to congestion.
 * <p>
 * ---
 * Let's do it in re-planning.
 * If doing so, we have the time to handle the result (on Carrier level) in the beforeMobsim step of the CarrierControlerListener.
 * kmt sep'22
 */
final class LSPRescheduler {
	private LSPRescheduler() {
	}

	static void notifyReplanning(LSPs lsps, ReplanningEvent event) {
		if (event.getIteration() != 0) {
			for (LSP lsp : lsps.getLSPs().values()) {
				for (LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
					solution.getShipments().clear();
					for (LogisticsSolutionElement element : solution.getSolutionElements()) {
						element.getIncomingShipments().clear();
						element.getOutgoingShipments().clear();
					}
				}

				for (LSPShipment shipment : lsp.getShipments()) {
					shipment.getShipmentPlan().clear();
					shipment.getLog().clear();
					lsp.getSelectedPlan().getAssigner().assignToSolution(shipment);
				}
				lsp.scheduleSolutions();
			}
		}
	}
}
