/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.LogisticChain;
import lsp.shipment.LSPShipment;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import java.util.*;

/*package-private*/ class EqualDistributionOfShipmentsStrategyFactory {

	/*package-private*/ EqualDistributionOfShipmentsStrategyFactory() {
	}

	/*package-private*/ GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<>());

		GenericPlanStrategyModule<LSPPlan> equalModule = new GenericPlanStrategyModule<>() {

			Integer shipmentCountBefore = 0;
			Integer shipmentCountAfter = 0;

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {

				//Ggf. könnte man hier auch den  ConsecutiveAssigner einklinken, der das Gleiche macht

				LSP lsp = lspPlan.getLSP();
				Map<LogisticChain, Integer> shipmentCountByChain = new LinkedHashMap<>();

//				LSPPlan initialPlan = lsp.getPlans().get(0);
//				for (LogisticChain logisticChain : initialPlan.getLogisticChains()) {
//					shipmentCountBefore += logisticChain.getShipmentIds().size();
//				}

				for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
					shipmentCountBefore += logisticChain.getShipmentIds().size();
					// ist der folgende Schritt nötig, da der bestehende Plan kopiert wird?
					logisticChain.getShipmentIds().clear();
				}

				for (LSPShipment shipment : lsp.getShipments()) {
					if (shipmentCountByChain.isEmpty()) {
						for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
							shipmentCountByChain.put(chain, 0);
						}
					}
					LogisticChain minChain = Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();
					minChain.assignShipment(shipment);
					shipmentCountByChain.merge(minChain, 1, Integer::sum);
				}

				for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
					shipmentCountAfter += logisticChain.getShipmentIds().size();
				}
			}

			@Override
			public void finishReplanning() {
//				if (!Objects.equals(shipmentCountBefore, shipmentCountAfter)) {
//					throw new RuntimeException("Shipments lost in replanning process");
//				}
			}
		};

		strategy.addStrategyModule(equalModule);
		return strategy;
	}

}
