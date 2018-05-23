package lsp.usecase;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;

public class DeterministicShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;
	
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
