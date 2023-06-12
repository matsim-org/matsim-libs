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
import java.util.Iterator;

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
				//TODO: Logistikketten können keine Shipments enthalten. Das wäre ein Problem beim entfernen.
				LSP lsp = lspPlan.getLSP();
				Id<LSPShipment> shipmentId = null;

				// iterate through initial plan chains and add shipment IDs to corresponding new plan chains
				for (LogisticChain initialPlanChain : lsp.getPlans().get(0).getLogisticChains()) {
					for (LogisticChain newPlanChain : lspPlan.getLogisticChains()) {
						if (newPlanChain.getId().equals(initialPlanChain.getId())) {
							newPlanChain.getShipmentIds().addAll(new ArrayList<>(initialPlanChain.getShipmentIds()));
							break;
						}
					}
				}

				int removeChainIndex;
				LogisticChain removeChain;
				Iterator<LogisticChain> removeChainIterator;
				do {
					removeChainIndex = MatsimRandom.getRandom().nextInt(lsp.getSelectedPlan().getLogisticChains().size());
					removeChainIterator = lsp.getSelectedPlan().getLogisticChains().iterator();
					for (int i = 0; i < removeChainIndex; i++) {
						removeChainIterator.next();
					}
					removeChain = removeChainIterator.next();
				} while (lsp.getSelectedPlan().getLogisticChains().iterator().next().getShipmentIds().isEmpty());

				int addChainIndex = MatsimRandom.getRandom().nextInt(lsp.getSelectedPlan().getLogisticChains().size());

				while (removeChainIndex == addChainIndex) {
					addChainIndex = MatsimRandom.getRandom().nextInt(lsp.getSelectedPlan().getLogisticChains().size());
				}

				Iterator<LogisticChain> iterator = lsp.getSelectedPlan().getLogisticChains().iterator();
				for (int i = 0; iterator.hasNext(); i++) {
					LogisticChain logisticChain = iterator.next();
					if (i == removeChainIndex) {
						shipmentId = logisticChain.getShipmentIds().iterator().next();
						logisticChain.getShipmentIds().remove(shipmentId);
						break;
					}
				}

				if (shipmentId != null) {
					iterator = lsp.getSelectedPlan().getLogisticChains().iterator();
					for (int i = 0; iterator.hasNext(); i++) {
						LogisticChain logisticChain = iterator.next();
						if (i == addChainIndex) {
							logisticChain.getShipmentIds().add(shipmentId);
							break;
						}
					}
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
