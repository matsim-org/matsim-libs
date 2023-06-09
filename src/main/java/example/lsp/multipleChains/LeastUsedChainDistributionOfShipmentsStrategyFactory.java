package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticChain;
import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class LeastUsedChainDistributionOfShipmentsStrategyFactory {


	LeastUsedChainDistributionOfShipmentsStrategyFactory() {
	}

	GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<>());
		GenericPlanStrategyModule<LSPPlan> leastUsageModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {
				LSP lsp = lspPlan.getLSP();

				Map<LogisticChain, Integer> shipmentCountByChain = new LinkedHashMap<>();
				LogisticChain minChain = null;
//				int index = MatsimRandom.getRandom().nextInt(lsp.getShipments().size());

				for (LogisticChain initialPlanChain : lsp.getPlans().get(0).getLogisticChains()) {
					for (LogisticChain newPlanChain : lspPlan.getLogisticChains()) {
						if (newPlanChain.getId().equals(initialPlanChain.getId())) {
							newPlanChain.getShipmentIds().addAll(new ArrayList<>(initialPlanChain.getShipmentIds()));
							break;
						}
					}
				}

				if (shipmentCountByChain.isEmpty()) {
					for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
						shipmentCountByChain.put(chain, 0);
					}
				}
				minChain = Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();
				//TODO: zufälliges Shipment der minChain hinzufügen und beim nullten Plan entsprechend entfernen
			}

			@Override
			public void finishReplanning() {
			}
		};

		strategy.addStrategyModule(leastUsageModule);
		return strategy;
	}
}
