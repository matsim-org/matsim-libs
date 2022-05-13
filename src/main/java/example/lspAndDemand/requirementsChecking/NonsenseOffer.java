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

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.offer.Offer;
import demand.offer.OfferVisitor;

/*package-private*/ class NonsenseOffer implements Offer{

	private LSPDecorator lsp;
	private LogisticsSolutionDecorator solution;
	
	
	@Override
	public LSPDecorator getLsp() {
		return lsp;
	}

	@Override
	public LogisticsSolutionDecorator getSolution() {
		return solution;
	}

	@Override
	public String getType() {
		return "nonsense";
	}

	@Override
	public void accept(OfferVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setLSP(LSPDecorator lsp) {
		this.lsp = lsp;
		
	}

	@Override
	public void setSolution(LogisticsSolutionDecorator solution) {
		this.solution = solution;
		
	}

}
