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

/*package-private*/ class RoundRobinDistributionAllShipmentsStrategyFactory {

	/*package-private*/ RoundRobinDistributionAllShipmentsStrategyFactory() {
	}

	/*package-private*/ GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<>());

		GenericPlanStrategyModule<LSPPlan> roundRobinModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {
				LSP lsp = lspPlan.getLSP();
				Map<LogisticChain, Integer> shipmentCountByChain = new LinkedHashMap<>();

				for (LSPShipment shipment : lsp.getShipments()) {
					if (shipmentCountByChain.isEmpty()) {
						for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
							shipmentCountByChain.put(chain, 0);
						}
					}
					LogisticChain minChain = Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue()).getKey();
					minChain.addShipmentToChain(shipment);
					shipmentCountByChain.merge(minChain, 1, Integer::sum);
				}
			}

			@Override
			public void finishReplanning() {
			}

		};

		strategy.addStrategyModule(roundRobinModule);
		return strategy;
	}

}
