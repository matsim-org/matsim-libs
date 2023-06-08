package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticChain;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import java.util.ArrayList;
import java.util.List;

class RandomDistributionOfShipmentsStrategyFactory {

	RandomDistributionOfShipmentsStrategyFactory() {
	}

	GenericPlanStrategy<LSPPlan, LSP> createStrategy() {

		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<>());
		GenericPlanStrategyModule<LSPPlan> randomModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {
				LSP lsp = lspPlan.getLSP();
				List<LogisticChain> logisticChains = new ArrayList<>(lsp.getSelectedPlan().getLogisticChains());

				for (LSPShipment shipment : lsp.getShipments()) {
					int index = MatsimRandom.getRandom().nextInt(logisticChains.size());
					logisticChains.get(index).addShipmentToChain(shipment);
				}
			}

			@Override
			public void finishReplanning() {
			}

		};

		strategy.addStrategyModule(randomModule);
		return strategy;
	}

}
