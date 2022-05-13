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

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.demandObject.DemandObject;
import demand.offer.Offer;
import demand.offer.OfferTransferrer;
import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

class RequirementsTransferrer implements OfferTransferrer{

	private LSPDecorator lsp;
	private final Collection<LogisticsSolutionDecorator> feasibleSolutions;
	
	public RequirementsTransferrer() {
		this.feasibleSolutions = new ArrayList<>();
	}
	
	
	@Override
	public Offer transferOffer(DemandObject object, String type, Id<LogisticsSolution> solutionId) {
		feasibleSolutions.clear();		
		label:			
			for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
					LogisticsSolutionDecorator solutionWithOffers = (LogisticsSolutionDecorator) solution;
					for(Requirement requirement : object.getRequirements()) {
						if(!requirement.checkRequirement(solutionWithOffers)) {
							continue label;
						}
					}
					feasibleSolutions.add(solutionWithOffers);
				}
				
			LogisticsSolutionDecorator chosenSolution = feasibleSolutions.iterator().next();
			return chosenSolution.getOffer(object, type);
		
	}

	@Override
	public void setLSP(LSPDecorator lsp) {
		this.lsp = lsp;
	}


	@Override
	public LSPDecorator getLSP() {
		return lsp;
	}
}
