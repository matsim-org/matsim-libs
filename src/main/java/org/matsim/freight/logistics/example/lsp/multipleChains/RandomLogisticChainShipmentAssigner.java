package org.matsim.freight.logistics.example.lsp.multipleChains;

import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.ShipmentAssigner;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The {@link LSPShipment} is assigned randomly to a {@link LogisticChain}.
 * The logistic chains of a plan are collected in a list.
 * The chain to which the shipment is to be assigned is selected by a seeded random index.
 * Requirements: There must be at least one logisticChain in the plan.
 */

class RandomLogisticChainShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	RandomLogisticChainShipmentAssigner() {
	}

	@Override
	public LSP getLSP() {
		throw new RuntimeException("not implemented");
	}

	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	@Override
	public void assignToPlan(LSPPlan lspPlan, LSPShipment shipment) {
		Gbl.assertIf(lspPlan.getLogisticChains().size() > 0);
		List<LogisticChain> logisticChains = new ArrayList<>(lspPlan.getLogisticChains());
		Random rand = MatsimRandom.getRandom();
		int index = rand.nextInt(logisticChains.size());
		LogisticChain logisticChain = logisticChains.get(index);
		logisticChain.addShipmentToChain(shipment);
	}
}

