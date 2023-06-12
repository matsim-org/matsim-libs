package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticChain;
import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.Id;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class RebalancingShipmentsStrategyFactory {


	RebalancingShipmentsStrategyFactory() {
	}

	GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<>());
		GenericPlanStrategyModule<LSPPlan> loadBalancingModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {
				LSP lsp = lspPlan.getLSP();
				Map<LogisticChain, Integer> shipmentCountByChain = new LinkedHashMap<>();
				LogisticChain minChain = null;
				LogisticChain maxChain = null;

				// iterate through initial plan chains and add shipment IDs to corresponding new plan chains
				for (LogisticChain initialPlanChain : lsp.getPlans().get(0).getLogisticChains()) {
					for (LogisticChain newPlanChain : lspPlan.getLogisticChains()) {
						if (newPlanChain.getId().equals(initialPlanChain.getId())) {
							newPlanChain.getShipmentIds().addAll(new ArrayList<>(initialPlanChain.getShipmentIds()));
							break;
						}
					}
				}

				// fill the shipmentCountByChain map with each chain's shipment count
				for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
					shipmentCountByChain.put(chain, chain.getShipmentIds().size());
				}

				// find the chains with the minimum and maximum shipment counts
				minChain = Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();
				maxChain = Collections.max(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();

				// get the first shipment ID from the chain with the maximum shipment count
				Id<LSPShipment> shipmentIdForReplanning = maxChain.getShipmentIds().iterator().next();

				// iterate through the chains and move the shipment from the max chain to the min chain
				for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
					if (logisticChain.equals(maxChain)) {
						logisticChain.getShipmentIds().remove(shipmentIdForReplanning);
					}
					if (logisticChain.equals(minChain)) {
						logisticChain.getShipmentIds().add(shipmentIdForReplanning);
					}
				}
			}

			@Override
			public void finishReplanning() {
			}
		};

		strategy.addStrategyModule(loadBalancingModule);
		return strategy;
	}
}
