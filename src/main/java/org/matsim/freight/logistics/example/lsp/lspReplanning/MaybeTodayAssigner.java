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

package org.matsim.freight.logistics.example.lsp.lspReplanning;

import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.ShipmentAssigner;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

import java.util.Random;

/*package-private*/ class MaybeTodayAssigner implements ShipmentAssigner {

	private final Random random;
	private LSP lsp;

	public MaybeTodayAssigner() {
		this.random = new Random(1);
	}

	@Override
	public void assignToPlan(LSPPlan lspPlan, LSPShipment shipment) {
		boolean assignToday = random.nextBoolean();
		if (assignToday) {
			Gbl.assertIf(lspPlan.getLogisticChains().size() == 1);
			lspPlan.getLogisticChains().iterator().next().addShipmentToChain(shipment);
		}
	}

	@Override public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}
	@Override public LSP getLSP(){
		throw new RuntimeException( "not implemented" );
	}

}
