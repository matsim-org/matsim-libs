package org.matsim.freight.logistics.example.lsp.multipleChains;

import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class ProximityStrategyFactory {

	private ProximityStrategyFactory() { } // class contains only static methods; do not instantiate

	static GenericPlanStrategy<LSPPlan, LSP> createStrategy( Network network ) {

		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
		GenericPlanStrategyModule<LSPPlan> randomModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {

				// Shifting shipments only makes sense for multiple chains
				if (lspPlan.getLogisticChains().size() < 2) return;

				LSP lsp = lspPlan.getLSP();
				double minDistance = Double.MAX_VALUE;
				LSPResource minDistanceResource = null;

				// get all shipments assigned to the LSP
				Map<Id<LSPShipment>, LSPShipment> shipmentById = new HashMap<>();
				for (LSPShipment lspShipment : lsp.getShipments()) {
					shipmentById.put(lspShipment.getId(), lspShipment);
				}

				// Retrieve all shipments in the logistic chains of the plan
				// These should be all shipments of the lsp, but not necessarily if shipments got lost
				ArrayList<LSPShipment> shipments = new ArrayList<>();
				for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
					for (Id<LSPShipment> id : logisticChain.getShipmentIds()) {
						LSPShipment shipment = shipmentById.get(id);
						if (shipment != null) {
							shipments.add(shipment);
						}
					}
				}

				// pick a random shipment from the shipments contained in the plan
				int shipmentIndex = MatsimRandom.getRandom().nextInt(shipments.size());
				LSPShipment shipment = shipments.get(shipmentIndex);

				// Collect all resources of the logistic chains of the LSP plan
				ArrayList<LSPResource> resources = new ArrayList<>();
				for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
					for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
						resources.add(logisticChainElement.getResource());
					}
				}

				// get the resource with the smallest distance to the shipment
				for (LSPResource resource : resources) {
					Link shipmentLink = network.getLinks().get(shipment.getTo());
					Link resourceLink = network.getLinks().get(resource.getStartLinkId());
					double distance = NetworkUtils.getEuclideanDistance(shipmentLink.getFromNode().getCoord(), resourceLink.getFromNode().getCoord());
					if (distance < minDistance) {
						minDistance = distance;
						minDistanceResource = resource;
					}
				}

				// add randomly picked shipment to chain with resource of the smallest distance
				for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
					for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
						if (logisticChainElement.getResource().equals(minDistanceResource)) {
							logisticChain.getShipmentIds().add(shipment.getId());
						}
					}
				}

				// remove the shipment from the previous logistic chain, can be the same as the new logistic chain
				for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
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
