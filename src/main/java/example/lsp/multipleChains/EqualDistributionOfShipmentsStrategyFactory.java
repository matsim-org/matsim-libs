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

import java.util.ArrayList;
import java.util.Collection;

/*package-private*/ class EqualDistributionOfShipmentsStrategyFactory {

	/*package-private*/ EqualDistributionOfShipmentsStrategyFactory() {
	}

	/*package-private*/ GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<>());

		GenericPlanStrategyModule<LSPPlan> tomorrowModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
				// TODO Auto-generated method stub
				// Todo  durchzählen vorher
			}

			@Override
			public void handlePlan(LSPPlan plan) {
//				LSP lsp = assigner.getLSP();
				LSP lsp = plan.getLSP();
				var bisherigeShipments = new ArrayList<>();
				for (LogisticChain logisticChain : plan.getLogisticChains()) {
					bisherigeShipments.add(logisticChain.getShipmentIds());
					logisticChain.getShipmentIds().clear();
				}
				Collection<LSPShipment> shipments = lsp.getShipments();
				for (LSPShipment shipment : shipments) {
					//Weise es den Ketten zu
				}
			}

			@Override
			public void finishReplanning() {
				// TODO Auto-generated method stub
				if (nuBefore != nuAfter) {
					throw new RuntimeException("Mist!");
				}

			}
		};

		strategy.addStrategyModule(tomorrowModule);
		return strategy;
	}

}
