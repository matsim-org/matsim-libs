package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LogisticChain;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@link LSPShipment} is assigned consecutively to a {@link LogisticChain}.
 * In case of one chain the shipment is assigned to that chain.
 * If there are more chains, the shipment is assigned to the chain which has the least shipments to this point and
 * thus distributes the shipments evenly in sequence across the logistics chains.
 * Requirements: There must be at least one logisticChain in the plan
 */

public class RoundRobinLogisticChainShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	// map of logistic chains and their number of assigned shipments in order of addition
	Map<LogisticChain, Integer> shipmentCountByChain = new LinkedHashMap<>();

	RoundRobinLogisticChainShipmentAssigner() {
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
		//prepare the map if empty for the first time with each number of assigned shipments being zero
		if(shipmentCountByChain.isEmpty()) {
			for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
				shipmentCountByChain.put(chain, 0);
			}
		}

		//assign the shipment to the chain with the least number of assigned shipments so far, increase its value by one
		LogisticChain minChain = Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();
		minChain.addShipmentToChain(shipment);
		shipmentCountByChain.merge(minChain, 1, Integer::sum);
	}

}