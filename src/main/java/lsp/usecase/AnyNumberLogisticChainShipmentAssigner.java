package lsp.usecase;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


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
		List<LogisticChain> logisticChains = new ArrayList<>(lsp.getSelectedPlan().getLogisticChains());
//		Random rand = MatsimRandom.getRandom();
		Random rand = new Random();
		int index = rand.nextInt(logisticChains.size());
		LogisticChain logisticChain = logisticChains.get(index);
		logisticChain.assignShipment(shipment);
//		if (lsp.getSelectedPlan().getLogisticChains().size() == 1) {
//			LogisticChain logisticChain = lsp.getSelectedPlan().getLogisticChains().iterator().next();
//			logisticChain.assignShipment(shipment);

			//for successive distribution of shipments over logistic chains in case number of logisticChains and shipments is equal
//		} else {
//			for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
//				if (logisticChain.getShipments().size() == 0) {
//					logisticChain.assignShipment(shipment);
//					break;
//				}
//			}
//		} else {
//			logisticChains.addAll(lsp.getSelectedPlan().getLogisticChains());
//			Random rand = MatsimRandom.getRandom();
////			Random rand = new Random();
//			int index = rand.nextInt(logisticChains.size());
//			LogisticChain logisticChain = logisticChains.get(index);
//			logisticChain.assignShipment(shipment);
//		}
	}
}

