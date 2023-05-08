package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@link LSPShipment} is assigned consecutively to a {@link LogisticChain}.
 * In case of one chain the shipment is assigned to that chain.
 * If there are more chains, the shipment is assigned to the chain which has the least shipments to this point and
 * thus distributes the shipments evenly in sequence across the logistics chains.
 * Requirements: There must be at least one logisticChain in the plan
 */

public class ConsecutiveLogisticChainShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	ConsecutiveLogisticChainShipmentAssigner() {
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

		// map of logistic chains and their number of assigned shipments in order of addition
		Map<LogisticChain, Integer> shipmentCountByChain = new LinkedHashMap<>();

		for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
			shipmentCountByChain.put(logisticChain, logisticChain.getShipments().size());
		}

		// variables for key and value to find logistic chain with the smallest number of assigned shipments
		LogisticChain minChain = null;
		Integer minSize = Integer.MAX_VALUE;

		for (Map.Entry<LogisticChain, Integer> e : shipmentCountByChain.entrySet()) {
			if (e.getValue() < minSize) {
				minChain = e.getKey();
				minSize = e.getValue();
			}
		}

		// assign the shipment to the logisticChain with the least number of assigner shipments so far
		if (minChain != null) {
			minChain.assignShipment(shipment);
		}
	}

}