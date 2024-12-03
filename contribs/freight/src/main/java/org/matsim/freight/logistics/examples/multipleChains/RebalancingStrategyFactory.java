/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2024 by the members listed in the COPYING,       *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.multipleChains;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 *  This strategy removes **one** randomly selected shipment from logistic chain with the most shipments and reassign it to one of the chains with the lowest number of shipments.
 *  This strategy allows to slowly change the plans and therefore follow the iterative learning process. But is moves towards a solution with all chains having the same number of shipments.
 *  For me (KMT) the use case is not obvious, when used as only strategy, but it can have its use in a set of strategies.
 *  @author nrichter (during his master thesis @VSP)
 */
class RebalancingStrategyFactory {
  //This is ok so as long as it is **non-public**.
  //Before making it public, it should be configurable either via config or Injection.
  //KMT, KN (Jan'24)

  private RebalancingStrategyFactory() {} // class contains only static methods; do not instantiate

  static GenericPlanStrategy<LSPPlan, LSP> createStrategy() {

    GenericPlanStrategyImpl<LSPPlan, LSP> strategy =
        new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
    GenericPlanStrategyModule<LSPPlan> loadBalancingModule =
        new GenericPlanStrategyModule<>() {

          @Override
          public void prepareReplanning(ReplanningContext replanningContext) {}

          @Override
          public void handlePlan(LSPPlan lspPlan) {

            // Shifting shipments only makes sense for multiple chains
            if (lspPlan.getLogisticChains().size() < 2) return;

            LSP lsp = lspPlan.getLSP();
            Map<LogisticChain, Integer> shipmentCountByChain = new HashMap<>();
            LogisticChain minChain;
            LogisticChain maxChain;

            // fill the shipmentCountByChain map with each chain's shipment count
            for (LogisticChain chain : lsp.getSelectedPlan().getLogisticChains()) {
              shipmentCountByChain.put(chain, chain.getLspShipmentIds().size());
            }

            // find the chains with the minimum and maximum shipment counts
            minChain =
                Collections.min(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue())
                    .getKey();
            maxChain =
                Collections.max(shipmentCountByChain.entrySet(), Map.Entry.comparingByValue())
                    .getKey();

            // If min and max chains are the same, no need to shift shipments
            if (minChain.equals(maxChain)) return;

            // get the first shipment ID from the chain with the maximum shipment count
            Id<LspShipment> shipmentIdForReplanning = maxChain.getLspShipmentIds().iterator().next();

            // iterate through the chains and move the shipment from the max chain to the min chain
            for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
              if (logisticChain.equals(maxChain)) {
                logisticChain.getLspShipmentIds().remove(shipmentIdForReplanning);
              }
              if (logisticChain.equals(minChain)) {
                logisticChain.getLspShipmentIds().add(shipmentIdForReplanning);
              }
            }
          }

          @Override
          public void finishReplanning() {}
        };

    strategy.addStrategyModule(loadBalancingModule);
    return strategy;
  }
}
