package org.matsim.freight.logistics.example.lsp.multipleChains;

import org.matsim.core.gbl.Gbl;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.ShipmentAssigner;
import org.matsim.freight.logistics.shipment.LSPShipment;

/**
 * The {@link LSPShipment} is assigned to the first {@link LogisticChain}.
 * In case of one chain the shipment is assigned to that chain.
 * If there are more chains, the shipment is assigned to the first of all chains.
 * Requirements: There must be at least one logisticChain in the plan
 */


class PrimaryLogisticChainShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	public PrimaryLogisticChainShipmentAssigner() {
	}

	@Override
	public LSP getLSP() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	@Override
	public void assignToPlan(LSPPlan lspPlan, LSPShipment shipment) {
		Gbl.assertIf(lspPlan.getLogisticChains().size() > 0);
		LogisticChain firstLogisticChain = lspPlan.getLogisticChains().iterator().next();
		firstLogisticChain.addShipmentToChain(shipment);
	}
}
