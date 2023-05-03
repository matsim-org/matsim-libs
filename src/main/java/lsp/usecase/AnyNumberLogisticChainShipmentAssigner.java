package lsp.usecase;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;


class AnyNumberLogisticChainShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	AnyNumberLogisticChainShipmentAssigner() {
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
		Gbl.assertIf(lsp.getSelectedPlan().getLogisticChains().size() != 0);
		if (lsp.getSelectedPlan().getLogisticChains().size() == 1) {
			LogisticChain logisticChain = lsp.getSelectedPlan().getLogisticChains().iterator().next();
			logisticChain.assignShipment(shipment);
		} else {
			for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
				if (logisticChain.getShipments().size() == 0) {
					logisticChain.assignShipment(shipment);
					break;
				}
			}
		}
	}

}

