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

package example.lsp.lspReplanning;

import java.util.Random;

import lsp.LSP;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

/*package-private*/ class MaybeTodayAssigner implements ShipmentAssigner{

	private LSP lsp;
	private final Random random;
	
	public MaybeTodayAssigner() {
		this.random = new Random(1);
	}
	
	@Override
	public void assignToSolution(LSPShipment shipment) {
		boolean assignToday = random.nextBoolean();
		if(assignToday) {
			Gbl.assertIf( lsp.getSelectedPlan().getSolutions().size()==1 );
			lsp.getSelectedPlan().getSolutions().iterator().next().assignShipment(shipment);
		}	
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
