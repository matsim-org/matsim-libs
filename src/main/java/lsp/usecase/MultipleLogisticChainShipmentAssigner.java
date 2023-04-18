package lsp.usecase;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;


class MultipleLogisticChainShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	MultipleLogisticChainShipmentAssigner() {
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
		for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChain()) {
			if (logisticChain.getShipments().size() == 0) {
				logisticChain.assignShipment(shipment);
				break;
			}
		}
	}

}

