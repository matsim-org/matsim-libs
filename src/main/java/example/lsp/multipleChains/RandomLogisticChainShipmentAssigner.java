package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
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
	public void assignToLogisticChain(LSPShipment shipment) {
		Gbl.assertIf(lsp.getSelectedPlan().getLogisticChains().size() > 0);
		List<LogisticChain> logisticChains = new ArrayList<>(lsp.getSelectedPlan().getLogisticChains());
		Random rand = MatsimRandom.getRandom();
		int index = rand.nextInt(logisticChains.size());
		LogisticChain logisticChain = logisticChains.get(index);
		logisticChain.assignShipment(shipment);
	}
}

