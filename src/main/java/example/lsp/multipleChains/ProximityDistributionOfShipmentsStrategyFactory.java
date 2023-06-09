package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;

class ProximityDistributionOfShipmentsStrategyFactory {


	ProximityDistributionOfShipmentsStrategyFactory() {
	}

	GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<>());
		GenericPlanStrategyModule<LSPPlan> proximityModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {
				LSP lsp = lspPlan.getLSP();

				//get shipments and their location from lsp
				for (LSPShipment shipment : lsp.getShipments()) {
					Id<Link> loc = shipment.getFrom();

				}
			}

			@Override
			public void finishReplanning() {
			}
		};

		strategy.addStrategyModule(proximityModule);
		return strategy;
	}
}
