package example.lsp.multipleChains;

import lsp.*;
import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import java.util.*;

class ProximityStrategyFactory {

	private final Network network;

	ProximityStrategyFactory(Network network) {
		this.network = network;
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
				double minDistance = Double.MAX_VALUE;
				LSPResource minDistanceResource = null;


				// iterate through initial plan chains and add shipment IDs to corresponding new plan chains
				for (LogisticChain initialPlanChain : lsp.getPlans().get(0).getLogisticChains()) {
					for (LogisticChain newPlanChain : lspPlan.getLogisticChains()) {
						if (newPlanChain.getId().equals(initialPlanChain.getId())) {
							newPlanChain.getShipmentIds().addAll(new ArrayList<>(initialPlanChain.getShipmentIds()));
							break;
						}
					}
				}

				// make a new list of shipments and pick a random shipment from it
				List<LSPShipment> shipments = new ArrayList<>(lsp.getShipments());
				int shipmentIndex = MatsimRandom.getRandom().nextInt(lsp.getShipments().size());
				LSPShipment shipment = shipments.get(shipmentIndex);

				// get the resource with the smallest distance to the shipment
				for (LSPResource resource : lsp.getResources()) {
					Link shipmentLink = network.getLinks().get(shipment.getTo());
					Link resourceLink = network.getLinks().get(resource.getStartLinkId());
					double distance = NetworkUtils.getEuclideanDistance(shipmentLink.getFromNode().getCoord(), resourceLink.getFromNode().getCoord());
					if (distance < minDistance) {
						minDistance = distance;
						minDistanceResource = resource;
					}
				}

				// add shipment to chain with resource of the smallest distance
				for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
					for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
						if (logisticChainElement.getResource().equals(minDistanceResource)) {
							logisticChain.getShipmentIds().add(shipment.getId());
						}
					}
				}

				// remove the shipment from the previous logistic chain, can be the same as the new logistic chain
				for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
					if (logisticChain.getShipmentIds().contains(shipment.getId())) {
						logisticChain.getShipmentIds().remove(shipment.getId());
						break;
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
