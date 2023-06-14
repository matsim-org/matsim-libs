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

package example.lspAndDemand.requirementsChecking;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import lsp.shipment.Requirement;

import java.util.ArrayList;
import java.util.Collection;

class RequirementsAssigner implements ShipmentAssigner {

	private final Collection<LogisticChain> feasibleLogisticChains;
	private LSP lsp;

	public RequirementsAssigner() {
		this.feasibleLogisticChains = new ArrayList<>();
	}

	@Override
	public void assignToLogisticChain(LSPShipment shipment) {
		feasibleLogisticChains.clear();

		label:
		for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChains()) {
			for (Requirement requirement : shipment.getRequirements()) {
				if (!requirement.checkRequirement(solution)) {

					continue label;
				}
			}
			feasibleLogisticChains.add(solution);
		}
		LogisticChain chosenSolution = feasibleLogisticChains.iterator().next();
		chosenSolution.addShipmentToChain(shipment);
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}
	@Override public LSP getLSP(){
		throw new RuntimeException( "not implemented" );
	}

}
