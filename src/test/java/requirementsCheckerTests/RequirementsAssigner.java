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

package requirementsCheckerTests;

import java.util.ArrayList;
import java.util.Collection;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import lsp.shipment.Requirement;

	public class RequirementsAssigner implements  ShipmentAssigner {

	private LSP lsp;
	private final Collection<LogisticsSolution> feasibleSolutions;
	
	public RequirementsAssigner() {
		this.feasibleSolutions = new ArrayList<>();
	}
	
	@Override
	public void assignToSolution(LSPShipment shipment) {
		feasibleSolutions.clear();
		
		label:
		for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			for(Requirement requirement : shipment.getRequirements()) {
				if(!requirement.checkRequirement(solution)) {
					
					continue label;
				}
			}
			feasibleSolutions.add(solution);
		}
		LogisticsSolution chosenSolution = feasibleSolutions.iterator().next();
		chosenSolution.assignShipment(shipment);
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

//	@Override
//	public LSP getLSP() {
//		return lsp;
//	}

}
