package lsp.usecase;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;

/*package-private*/ class DeterministicShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	DeterministicShipmentAssigner() {
	}

	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}
	
	@Override
	public void assignShipment(LSPShipment shipment) {
		LogisticsSolution singleSolution = lsp.getSelectedPlan().getSolutions().iterator().next();
		singleSolution.assignShipment(shipment);
	}

	@Override
	public LSP getLSP() {
		return lsp;
	}

}
