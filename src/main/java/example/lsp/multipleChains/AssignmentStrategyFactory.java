package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticChain;
import lsp.LogisticChainElement;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

@Deprecated
public class AssignmentStrategyFactory {

	public AssignmentStrategyFactory() {
	}

	public GenericPlanStrategy<LSPPlan, LSP> createStrategy() {

		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new RandomPlanSelector<>());
		GenericPlanStrategyModule<LSPPlan> assignmentModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {

				for (LogisticChain solution : lspPlan.getLogisticChains()) {
					solution.getShipmentIds().clear();
					for (LogisticChainElement element : solution.getLogisticChainElements()) {
						element.getIncomingShipments().clear();
						element.getOutgoingShipments().clear();
					}
				}

				for (LSPShipment shipment : lspPlan.getLSP().getShipments()) {
					ShipmentUtils.getOrCreateShipmentPlan(lspPlan, shipment.getId()).clear();
					shipment.getShipmentLog().clear();
					lspPlan.getAssigner().assignToPlan(lspPlan, shipment);
				}
			}

			@Override
			public void finishReplanning() {
			}

		};

		strategy.addStrategyModule(assignmentModule);
		return strategy;
	}

}
