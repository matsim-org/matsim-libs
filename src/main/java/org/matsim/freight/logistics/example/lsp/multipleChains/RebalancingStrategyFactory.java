package org.matsim.freight.logistics.example.lsp.multipleChains;

import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class RebalancingStrategyFactory {


	RebalancingStrategyFactory() {
	}

	GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
		GenericPlanStrategyModule<LSPPlan> loadBalancingModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {

				// Shifting shipments only makes sense for multiple chains
				if (lspPlan.getLogisticChains().size() < 2) return;

				LSP lsp = lspPlan.getLSP();
				Map<LogisticChain, Integer> shipmentCountByChain = new HashMap<>();
				LogisticChain minChain;
				LogisticChain maxChain;

				// fill the shipmentCountByChain map with each chain's shipment count
				for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
					shipmentCountByChain.put(chain, chain.getShipmentIds().size());
				}

				// find the chains with the minimum and maximum shipment counts
				minChain = Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();
				maxChain = Collections.max(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();

				// If min and max chains are the same, no need to shift shipments
				if(minChain.equals(maxChain)) return;

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
