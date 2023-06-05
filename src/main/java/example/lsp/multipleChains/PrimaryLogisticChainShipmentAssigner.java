package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

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
	public void assignToLogisticChain(LSPShipment shipment) {
		Gbl.assertIf(lsp.getSelectedPlan().getLogisticChains().size() > 0);
		LogisticChain firstLogisticChain = lsp.getSelectedPlan().getLogisticChains().iterator().next();
		firstLogisticChain.assignShipment(shipment);
	}
}
