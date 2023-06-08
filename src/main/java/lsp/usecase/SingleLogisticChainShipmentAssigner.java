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

package lsp.usecase;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

/**
 * Ganz einfacher {@link ShipmentAssigner}:
 * Voraussetzung: Der {@link lsp.LSPPlan} hat genau 1 {@link LogisticChain}.
 * <p>
 * Dann wird das {@link  LSPShipment} diesem zugeordnet.
 * <p>
 * (Falls die Voraussetzung "exakt 1 LogisticChain pro Plan" nicht erfüllt ist, kommt eine RuntimeException)
 */
class SingleLogisticChainShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	SingleLogisticChainShipmentAssigner() {
	}

	@Override
	public LSP getLSP() {
		throw new RuntimeException("not implemented");
	}

	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	@Override
	public void assignToLogisticChain(LSPShipment shipment) {
		Gbl.assertIf(lsp.getSelectedPlan().getLogisticChains().size() == 1);
		LogisticChain singleSolution = lsp.getSelectedPlan().getLogisticChains().iterator().next();
		singleSolution.addShipmentToChain(shipment);
	}

}
