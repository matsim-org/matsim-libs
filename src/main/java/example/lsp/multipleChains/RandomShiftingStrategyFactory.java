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
import java.util.Iterator;
import java.util.List;

class RandomShiftingStrategyFactory {

	RandomShiftingStrategyFactory() {
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

				// iterate through initial plan chains and add shipment IDs to corresponding new plan chains
				for (LogisticChain initialPlanChain : lsp.getPlans().get(0).getLogisticChains()) {
					for (LogisticChain newPlanChain : lspPlan.getLogisticChains()) {
						if (newPlanChain.getId().equals(initialPlanChain.getId())) {
							newPlanChain.getShipmentIds().addAll(new ArrayList<>(initialPlanChain.getShipmentIds()));
							break;
						}
					}
				}

				// Make a new list of shipments and pick a random shipment from it
				List<LSPShipment> shipments = new ArrayList<>(lsp.getShipments());
				int shipmentIndex = MatsimRandom.getRandom().nextInt(lsp.getShipments().size());
				LSPShipment shipment = shipments.get(shipmentIndex);

				// Find and remove the random shipment from its current logistic chain
				LogisticChain sourceLogisticChain = null;
				for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
					if (logisticChain.getShipmentIds().remove(shipment.getId())) {
						sourceLogisticChain = logisticChain;
						break;
					}
				}

				// Find a new logistic chain for the shipment
				// Ensure that the chain selected is not the same as the one it was removed from
				int chainIndex;
				LogisticChain targetLogisticChain = null;
				do {
					chainIndex = MatsimRandom.getRandom().nextInt(lsp.getSelectedPlan().getLogisticChains().size());
					Iterator<LogisticChain> iterator = lsp.getSelectedPlan().getLogisticChains().iterator();
					for (int i = 0; iterator.hasNext(); i++) {
						targetLogisticChain = iterator.next();
						if (i == chainIndex) {
							break;
						}
					}
				} while (targetLogisticChain == sourceLogisticChain);

				// Add the shipment to the new logistic chain
				targetLogisticChain.addShipmentToChain(shipment);
			}

			@Override
			public void finishReplanning() {
			}

		};

		strategy.addStrategyModule(randomModule);
		return strategy;
	}

}
